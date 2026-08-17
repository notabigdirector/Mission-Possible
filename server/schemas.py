from __future__ import annotations

from typing import List, Literal, Optional

from pydantic import BaseModel


class TaskRecord(BaseModel):
    id: str
    parentId: Optional[str] = None
    projectId: Optional[str] = None
    title: str
    description: str
    status: str
    priority: str
    dueAt: Optional[int] = None
    createdAt: int
    updatedAt: int


class ProjectRecord(BaseModel):
    id: str
    name: str
    priority: int
    createdAt: int
    updatedAt: int


class Tombstone(BaseModel):
    kind: Literal["task", "project"]
    id: str
    updatedAt: int


class SyncRequest(BaseModel):
    tasks: List[TaskRecord] = []
    projects: List[ProjectRecord] = []
    deleted: List[Tombstone] = []


class SyncResponse(BaseModel):
    tasks: List[TaskRecord] = []
    projects: List[ProjectRecord] = []
    tombstones: List[Tombstone] = []


class RegisterRequest(BaseModel):
    name: str


class UserInfo(BaseModel):
    id: str
    name: str


class RegisterResponse(BaseModel):
    token: str
    user: UserInfo


class HealthResponse(BaseModel):
    ok: bool
