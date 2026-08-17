from fastapi.testclient import TestClient

from main import app

client = TestClient(app)


def _register(name="user") -> dict:
    resp = client.post("/api/v1/register", json={"name": name})
    assert resp.status_code == 200
    return resp.json()


def _auth(token: str) -> dict:
    return {"Authorization": f"Bearer {token}"}


def _task(id, updated_at, title="t", **overrides):
    base = {
        "id": id,
        "parentId": None,
        "projectId": None,
        "title": title,
        "description": "",
        "status": "not_started",
        "priority": "medium",
        "dueAt": None,
        "createdAt": updated_at,
        "updatedAt": updated_at,
    }
    base.update(overrides)
    return base


def _project(id, updated_at, name="p", **overrides):
    base = {
        "id": id,
        "name": name,
        "priority": 0,
        "createdAt": updated_at,
        "updatedAt": updated_at,
    }
    base.update(overrides)
    return base


def test_health():
    resp = client.get("/api/v1/health")
    assert resp.status_code == 200
    assert resp.json() == {"ok": True}


def test_register_returns_token_and_user():
    data = _register("alice")
    assert data["token"]
    assert data["user"]["name"] == "alice"
    assert data["user"]["id"]


def test_sync_requires_auth():
    resp = client.post("/api/v1/sync", json={"tasks": [], "projects": [], "deleted": []})
    assert resp.status_code == 401
    resp = client.post(
        "/api/v1/sync", json={"tasks": [], "projects": [], "deleted": []}, headers=_auth("bogus")
    )
    assert resp.status_code == 401


def test_first_sync_preserves_existing_local_tasks():
    user = _register()
    headers = _auth(user["token"])
    task = _task("t1", 1000)
    project = _project("p1", 1000)
    resp = client.post(
        "/api/v1/sync",
        json={"tasks": [task], "projects": [project], "deleted": []},
        headers=headers,
    )
    assert resp.status_code == 200
    body = resp.json()
    assert body["tasks"] == [task]
    assert body["projects"] == [project]
    assert body["tombstones"] == []


def test_last_write_wins_for_task():
    user = _register()
    headers = _auth(user["token"])
    client.post("/api/v1/sync", json={"tasks": [_task("t1", 1000, title="old")], "deleted": []}, headers=headers)
    resp = client.post("/api/v1/sync", json={"tasks": [_task("t1", 2000, title="new")], "deleted": []}, headers=headers)
    assert resp.json()["tasks"][0]["title"] == "new"
    resp = client.post("/api/v1/sync", json={"tasks": [_task("t1", 1500, title="stale")], "deleted": []}, headers=headers)
    assert resp.json()["tasks"][0]["title"] == "new"


def test_tombstone_delete_propagates():
    user = _register()
    headers = _auth(user["token"])
    client.post("/api/v1/sync", json={"tasks": [_task("t1", 1000)], "deleted": []}, headers=headers)
    resp = client.post(
        "/api/v1/sync",
        json={"tasks": [], "deleted": [{"kind": "task", "id": "t1", "updatedAt": 1500}]},
        headers=headers,
    )
    body = resp.json()
    assert body["tasks"] == []
    assert {"kind": "task", "id": "t1", "updatedAt": 1500} in body["tombstones"]


def test_deleted_task_stays_deleted_for_other_client():
    user = _register()
    headers = _auth(user["token"])
    client.post("/api/v1/sync", json={"tasks": [_task("t1", 1000)], "deleted": []}, headers=headers)
    client.post(
        "/api/v1/sync",
        json={"tasks": [], "deleted": [{"kind": "task", "id": "t1", "updatedAt": 1500}]},
        headers=headers,
    )
    resp = client.post("/api/v1/sync", json={"tasks": [_task("t1", 1200)], "deleted": []}, headers=headers)
    body = resp.json()
    assert body["tasks"] == []
    assert len(body["tombstones"]) == 1


def test_newer_live_record_undeletes():
    user = _register()
    headers = _auth(user["token"])
    client.post("/api/v1/sync", json={"tasks": [_task("t1", 1000)], "deleted": []}, headers=headers)
    client.post(
        "/api/v1/sync",
        json={"tasks": [], "deleted": [{"kind": "task", "id": "t1", "updatedAt": 1500}]},
        headers=headers,
    )
    resp = client.post("/api/v1/sync", json={"tasks": [_task("t1", 2000)], "deleted": []}, headers=headers)
    body = resp.json()
    assert len(body["tasks"]) == 1
    assert body["tombstones"] == []


def test_users_are_isolated():
    alice = _register("alice")
    bob = _register("bob")
    client.post(
        "/api/v1/sync", json={"tasks": [_task("shared", 1000)], "deleted": []}, headers=_auth(alice["token"])
    )
    resp = client.post(
        "/api/v1/sync", json={"tasks": [], "projects": [], "deleted": []}, headers=_auth(bob["token"])
    )
    assert resp.json()["tasks"] == []
    resp = client.post(
        "/api/v1/sync", json={"tasks": [], "projects": [], "deleted": []}, headers=_auth(alice["token"])
    )
    assert len(resp.json()["tasks"]) == 1


def test_project_merge_and_tombstone():
    user = _register()
    headers = _auth(user["token"])
    client.post("/api/v1/sync", json={"projects": [_project("p1", 1000)], "deleted": []}, headers=headers)
    resp = client.post(
        "/api/v1/sync",
        json={"projects": [], "deleted": [{"kind": "project", "id": "p1", "updatedAt": 1500}]},
        headers=headers,
    )
    body = resp.json()
    assert body["projects"] == []
    assert {"kind": "project", "id": "p1", "updatedAt": 1500} in body["tombstones"]


def test_multi_client_convergence():
    alice = _register("alice")
    bob = _register("bob")
    client.post("/api/v1/sync", json={"tasks": [_task("t1", 1000, title="a")], "deleted": []}, headers=_auth(alice["token"]))
    client.post("/api/v1/sync", json={"tasks": [_task("t2", 1100, title="b")], "deleted": []}, headers=_auth(bob["token"]))
    resp = client.post("/api/v1/sync", json={"tasks": [_task("t1", 1000, title="a"), _task("t2", 1100, title="b")], "deleted": []}, headers=_auth(alice["token"]))
    assert {t["id"] for t in resp.json()["tasks"]} == {"t1", "t2"}
