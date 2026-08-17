from __future__ import annotations

import sqlite3
import threading
from pathlib import Path
from typing import Any, Dict, List, Optional

from auth import hash_token

SCHEMA = """
CREATE TABLE IF NOT EXISTS users (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    token_hash TEXT NOT NULL UNIQUE,
    created_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS tasks (
    id TEXT NOT NULL,
    user_id TEXT NOT NULL,
    parent_id TEXT,
    project_id TEXT,
    title TEXT NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    status TEXT NOT NULL,
    priority TEXT NOT NULL,
    due_at INTEGER,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    deleted_at INTEGER,
    PRIMARY KEY (id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_tasks_user ON tasks(user_id);

CREATE TABLE IF NOT EXISTS projects (
    id TEXT NOT NULL,
    user_id TEXT NOT NULL,
    name TEXT NOT NULL,
    priority INTEGER NOT NULL,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    deleted_at INTEGER,
    PRIMARY KEY (id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_projects_user ON projects(user_id);
"""


class Database:
    def __init__(self, path: str) -> None:
        self.path = path
        self._lock = threading.Lock()
        Path(path).parent.mkdir(parents=True, exist_ok=True)
        with self._conn() as conn:
            conn.executescript(SCHEMA)

    def _conn(self) -> sqlite3.Connection:
        conn = sqlite3.connect(self.path, timeout=10)
        conn.row_factory = sqlite3.Row
        conn.execute("PRAGMA journal_mode=WAL")
        conn.execute("PRAGMA busy_timeout=10000")
        return conn

    # ---- users ----

    def create_user(self, user_id: str, name: str, token: str, created_at: int) -> Dict[str, Any]:
        token_hash = hash_token(token)
        with self._lock:
            with self._conn() as conn:
                conn.execute(
                    "INSERT INTO users (id, name, token_hash, created_at) VALUES (?, ?, ?, ?)",
                    (user_id, name, token_hash, created_at),
                )
        return {"id": user_id, "name": name}

    def get_user_by_token(self, token: str) -> Optional[Dict[str, Any]]:
        token_hash = hash_token(token)
        with self._conn() as conn:
            row = conn.execute(
                "SELECT id, name, created_at FROM users WHERE token_hash = ?", (token_hash,)
            ).fetchone()
        return dict(row) if row else None

    def user_exists(self, user_id: str) -> bool:
        with self._conn() as conn:
            row = conn.execute("SELECT 1 FROM users WHERE id = ?", (user_id,)).fetchone()
        return row is not None

    # ---- sync state ----

    def load_state(self, user_id: str) -> Dict[str, List[Dict[str, Any]]]:
        with self._conn() as conn:
            task_rows = conn.execute(
                "SELECT * FROM tasks WHERE user_id = ?", (user_id,)
            ).fetchall()
            project_rows = conn.execute(
                "SELECT * FROM projects WHERE user_id = ?", (user_id,)
            ).fetchall()
        return {
            "tasks": [self._task_to_dict(r) for r in task_rows],
            "projects": [self._project_to_dict(r) for r in project_rows],
        }

    def save_state(self, user_id: str, tasks: List[Dict[str, Any]], projects: List[Dict[str, Any]]) -> None:
        with self._lock:
            with self._conn() as conn:
                conn.execute("DELETE FROM tasks WHERE user_id = ?", (user_id,))
                conn.execute("DELETE FROM projects WHERE user_id = ?", (user_id,))
                conn.executemany(
                    """
                    INSERT INTO tasks (id, user_id, parent_id, project_id, title, description,
                                       status, priority, due_at, created_at, updated_at, deleted_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    [
                        (
                            t["id"],
                            user_id,
                            t.get("parentId"),
                            t.get("projectId"),
                            t["title"],
                            t.get("description", ""),
                            t["status"],
                            t["priority"],
                            t.get("dueAt"),
                            t["createdAt"],
                            t["updatedAt"],
                            t.get("deletedAt"),
                        )
                        for t in tasks
                    ],
                )
                conn.executemany(
                    """
                    INSERT INTO projects (id, user_id, name, priority, created_at, updated_at, deleted_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """,
                    [
                        (
                            p["id"],
                            user_id,
                            p["name"],
                            p["priority"],
                            p["createdAt"],
                            p["updatedAt"],
                            p.get("deletedAt"),
                        )
                        for p in projects
                    ],
                )

    @staticmethod
    def _task_to_dict(row: sqlite3.Row) -> Dict[str, Any]:
        return {
            "id": row["id"],
            "parentId": row["parent_id"],
            "projectId": row["project_id"],
            "title": row["title"],
            "description": row["description"],
            "status": row["status"],
            "priority": row["priority"],
            "dueAt": row["due_at"],
            "createdAt": row["created_at"],
            "updatedAt": row["updated_at"],
            "deletedAt": row["deleted_at"],
        }

    @staticmethod
    def _project_to_dict(row: sqlite3.Row) -> Dict[str, Any]:
        return {
            "id": row["id"],
            "name": row["name"],
            "priority": row["priority"],
            "createdAt": row["created_at"],
            "updatedAt": row["updated_at"],
            "deletedAt": row["deleted_at"],
        }
