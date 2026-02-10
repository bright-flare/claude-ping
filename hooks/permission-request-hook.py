#!/usr/bin/env python3

"""
ClaudePing - PermissionRequest Hook (Python 버전)
jq 없이 Python 표준 라이브러리만으로 동작합니다.
"""

import json
import sys
import os
import subprocess
from datetime import datetime
from pathlib import Path

# 설정
API_URL = "http://localhost:8080/api/hook/ask"
LOG_FILE = Path.home() / ".claude" / "claudeping.log"

# 로그 디렉토리 생성
LOG_FILE.parent.mkdir(parents=True, exist_ok=True)

def log(message):
    """로그 파일에 기록"""
    timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    with open(LOG_FILE, "a") as f:
        f.write(f"[{timestamp}] {message}\n")

def generate_question(tool_name, tool_input):
    """도구 타입에 따라 질문 생성"""
    if tool_name == "Bash":
        command = tool_input.get("command", "unknown command")
        description = tool_input.get("description", "No description")
        return f"Bash 명령을 실행하시겠습니까?\n\n명령: {command}\n설명: {description}"

    elif tool_name in ["Write", "Edit"]:
        file_path = tool_input.get("file_path", "unknown file")
        return f"파일을 수정하시겠습니까?\n\n파일: {file_path}"

    elif tool_name == "Read":
        file_path = tool_input.get("file_path", "unknown file")
        return f"파일을 읽으시겠습니까?\n\n파일: {file_path}"

    else:
        return f"{tool_name} 작업을 수행하시겠습니까?"

def call_api(question, context):
    """Spring Boot API 호출"""
    import urllib.request
    import urllib.error

    payload = {
        "event": "permission_request",
        "question": question,
        "context": context
    }

    try:
        req = urllib.request.Request(
            API_URL,
            data=json.dumps(payload).encode('utf-8'),
            headers={'Content-Type': 'application/json'},
            method='POST'
        )

        # 타임아웃 10분
        with urllib.request.urlopen(req, timeout=600) as response:
            return json.loads(response.read().decode('utf-8'))

    except urllib.error.HTTPError as e:
        log(f"HTTP Error: {e.code} {e.reason}")
        raise
    except urllib.error.URLError as e:
        log(f"URL Error: {e.reason}")
        raise
    except Exception as e:
        log(f"Unexpected error: {e}")
        raise

def main():
    log("=== PermissionRequest Hook triggered (Python) ===")

    try:
        # stdin에서 JSON 읽기
        input_data = json.load(sys.stdin)
        log(f"Input JSON: {json.dumps(input_data)}")

        # 데이터 추출
        tool_name = input_data.get("tool_name", "Unknown")
        tool_input = input_data.get("tool_input", {})
        session_id = input_data.get("session_id", "unknown")
        cwd = input_data.get("cwd", "unknown")

        log(f"Tool: {tool_name}")
        log(f"Input: {json.dumps(tool_input)}")

        # 질문 생성
        question = generate_question(tool_name, tool_input)
        log(f"Generated question: {question}")

        # API 호출 컨텍스트
        context = {
            "tool_name": tool_name,
            "tool_input": tool_input,
            "session_id": session_id,
            "cwd": cwd,
            "timestamp": datetime.utcnow().strftime("%Y-%m-%dT%H:%M:%SZ")
        }

        log("Sending request to ClaudePing API...")

        # API 호출
        response = call_api(question, context)

        log(f"API Response: {json.dumps(response)}")

        approved = response.get("approved", False)
        message = response.get("message", "No message")
        request_id = response.get("requestId", "unknown")

        log(f"Approved: {approved}, Message: {message}, RequestID: {request_id}")

        # Claude Code에 JSON 응답 반환
        if approved:
            output = {
                "hookSpecificOutput": {
                    "hookEventName": "PermissionRequest",
                    "decision": {
                        "behavior": "allow"
                    }
                }
            }
            print(json.dumps(output))
            log("Permission ALLOWED")
            sys.exit(0)
        else:
            output = {
                "hookSpecificOutput": {
                    "hookEventName": "PermissionRequest",
                    "decision": {
                        "behavior": "deny",
                        "message": message,
                        "interrupt": False
                    }
                }
            }
            print(json.dumps(output))
            log("Permission DENIED")
            sys.exit(0)

    except json.JSONDecodeError as e:
        error_msg = f"Invalid JSON input: {e}"
        log(error_msg)
        print(error_msg, file=sys.stderr)
        sys.exit(2)

    except Exception as e:
        error_msg = f"Error: {e}"
        log(error_msg)
        print(error_msg, file=sys.stderr)
        sys.exit(2)

if __name__ == "__main__":
    main()
