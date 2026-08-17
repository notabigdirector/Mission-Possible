import os
import sys
import tempfile
from pathlib import Path

import pytest

# 让 server 根目录可被导入
SERVER_DIR = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(SERVER_DIR))


@pytest.fixture()
def env(tmp_path):
    db_path = tmp_path / "test.db"
    old = os.environ.get("DB_PATH")
    os.environ["DB_PATH"] = str(db_path)
    yield db_path
    if old is None:
        os.environ.pop("DB_PATH", None)
    else:
        os.environ["DB_PATH"] = old
