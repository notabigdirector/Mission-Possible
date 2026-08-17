from __future__ import annotations

from dataclasses import dataclass
from typing import Any, Dict, List

from schemas import ProjectRecord, SyncRequest, SyncResponse, TaskRecord, Tombstone


def _merge_kind(
    existing: List[Dict[str, Any]],
    incoming: List[Dict[str, Any]],
    tombstones: List[Dict[str, Any]],
) -> List[Dict[str, Any]]:
    """Last-write-wins merge for one record kind.

    - Live records overwrite the stored record only when strictly newer.
    - Tombstones mark the stored record deleted only when strictly newer.
    - Records not sent by this client are kept as-is (the server is a
      superset that has seen every other syncing client).
    """
    by_id: Dict[str, Dict[str, Any]] = {r["id"]: dict(r) for r in existing}

    for record in incoming:
        current = by_id.get(record["id"])
        if current is None or record["updatedAt"] > current["updatedAt"]:
            by_id[record["id"]] = dict(record)

    for tomb in tombstones:
        current = by_id.get(tomb["id"])
        if current is None or tomb["updatedAt"] > current["updatedAt"]:
            deleted = dict(current) if current else {"createdAt": 0}
            deleted["id"] = tomb["id"]
            deleted["updatedAt"] = tomb["updatedAt"]
            deleted["deletedAt"] = tomb["updatedAt"]
            by_id[tomb["id"]] = deleted

    return list(by_id.values())


@dataclass
class MergeResult:
    stored_tasks: List[Dict[str, Any]]
    stored_projects: List[Dict[str, Any]]
    response: SyncResponse


def merge(req: SyncRequest, existing_tasks: List[Dict[str, Any]], existing_projects: List[Dict[str, Any]]) -> MergeResult:
    task_tombs: List[Dict[str, Any]] = []
    project_tombs: List[Dict[str, Any]] = []
    for tomb in req.deleted:
        target = task_tombs if tomb.kind == "task" else project_tombs
        target.append(tomb.model_dump())

    merged_tasks = _merge_kind(existing_tasks, [t.model_dump() for t in req.tasks], task_tombs)
    merged_projects = _merge_kind(
        existing_projects, [p.model_dump() for p in req.projects], project_tombs
    )

    live_tasks = [t for t in merged_tasks if not t.get("deletedAt")]
    live_projects = [p for p in merged_projects if not p.get("deletedAt")]
    tombstones: List[Tombstone] = [
        Tombstone(kind="task", id=t["id"], updatedAt=t["updatedAt"])
        for t in merged_tasks
        if t.get("deletedAt")
    ]
    tombstones.extend(
        Tombstone(kind="project", id=p["id"], updatedAt=p["updatedAt"])
        for p in merged_projects
        if p.get("deletedAt")
    )

    response = SyncResponse(
        tasks=[TaskRecord(**t) for t in live_tasks],
        projects=[ProjectRecord(**p) for p in live_projects],
        tombstones=tombstones,
    )
    return MergeResult(
        stored_tasks=merged_tasks,
        stored_projects=merged_projects,
        response=response,
    )
