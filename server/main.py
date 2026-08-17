from __future__ import annotations

import os
import time
import uuid
from typing import Optional

from fastapi import Depends, FastAPI, Header, HTTPException, status

from auth import generate_token
from db import Database
from schemas import (
    HealthResponse,
    RegisterRequest,
    RegisterResponse,
    SyncRequest,
    SyncResponse,
    UserInfo,
)
from sync import merge

app = FastAPI(title="Mission Sync Server", version="1.0.0")

DEFAULT_DB_PATH = os.path.join(os.path.dirname(__file__), "data", "sync.db")

_db: Optional[Database] = None


def _get_db() -> Database:
    global _db
    path = os.environ.get("DB_PATH", DEFAULT_DB_PATH)
    if _db is None or _db.path != path:
        _db = Database(path)
    return _db


def get_user(
    authorization: Optional[str] = Header(None),
    db: Database = Depends(_get_db),
):
    if not authorization or not authorization.lower().startswith("bearer "):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="缺少认证令牌")
    token = authorization.split(" ", 1)[1].strip()
    user = db.get_user_by_token(token)
    if user is None:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="令牌无效")
    return user


@app.get("/api/v1/health", response_model=HealthResponse)
def health() -> HealthResponse:
    return HealthResponse(ok=True)


@app.post("/api/v1/register", response_model=RegisterResponse)
def register(body: RegisterRequest, db: Database = Depends(_get_db)):
    name = body.name.strip()
    if not name:
        raise HTTPException(status_code=status.HTTP_422_UNPROCESSABLE_ENTITY, detail="用户名不能为空")
    token = generate_token()
    user_id = str(uuid.uuid4())
    db.create_user(user_id, name, token, int(time.time() * 1000))
    return RegisterResponse(token=token, user=UserInfo(id=user_id, name=name))


@app.post("/api/v1/sync", response_model=SyncResponse)
def sync(body: SyncRequest, user: dict = Depends(get_user), db: Database = Depends(_get_db)):
    state = db.load_state(user["id"])
    result = merge(body, state["tasks"], state["projects"])
    db.save_state(user["id"], result.stored_tasks, result.stored_projects)
    return result.response
