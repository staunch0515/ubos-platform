import streamlit as st
import requests
import json
from openai import OpenAI
import pandas as pd
from datetime import datetime

# ==========================================
# 1. Global Configuration
# ==========================================

UBOS_API_URL = "http://localhost:8080/api"

client = OpenAI(api_key="sk-proj-xxx")
MODEL_NAME = "gpt-4o"

st.set_page_config(page_title="UBOS Copilot", page_icon="🧬", layout="wide")
st.title("🧬 UBOS Control Console")

# ==========================================
# 2. Helper Functions (UBOS API Interaction)
# ==========================================

# ubos-copilot/app.py

def call_ubos(url, method="POST", payload=None):
    """Generic wrapper for UBOS API calls."""
    try:
        if method == "POST":
            resp = requests.post(url, json=payload, timeout=10)
        else:
            resp = requests.get(url, timeout=10)

        resp.raise_for_status() # Raise HTTPError for bad responses

        # --- FIX: Return Headers along with JSON ---
        json_data = resp.json() if resp.text else {}

        # Return a dictionary containing data and headers
        return {"data": json_data, "headers": dict(resp.headers)}
        # --- END FIX ---

    except requests.exceptions.RequestException as e:
        return {"error_connection": str(e), "status_code": getattr(e.response, 'status_code', 0)}

def run_logic(slug, context, branch="master"):
    """Execute logic within UBOS."""
    return call_ubos(f"{UBOS_API_URL}/run/{slug}?branch={branch}", payload=context)

# --- Audit API Helpers ---
def get_processes():
    return call_ubos(f"{UBOS_API_URL}/audit/processes", method="GET")

def get_process_detail(pid):
    return call_ubos(f"{UBOS_API_URL}/audit/process/{pid}", method="GET")

# ==========================================
# 3. Universal Renderer (SDUI)
# ==========================================

def handle_action(action_def):
    """Handle UI button actions (triggering backend logic)"""
    target_logic = action_def.get("target_logic")
    if target_logic:
        form_keys = ["slug", "name", "capital", "empId", "salary", "score", "userId", "basePrice", "device", "a", "b"]
        form_data = {k: st.session_state.get(k) for k in form_keys if st.session_state.get(k) is not None}

        with st.spinner(f"Calling Kernel: {target_logic}..."):
            result = run_logic(target_logic, form_data, branch="master")

        if "status" in result and result["status"] == "success":
            st.success("✅ Execution Successful")
            with st.expander("View Details"):
                st.json(result)
            st.rerun()
        else:
            st.error("❌ Execution Failed")
            st.json(result)

def render_component(comp, prefix=""):
    """Recursively render a single UI component (Only input/button supported for demo)"""
    c_type = comp.get("type")
    c_label = comp.get("label", "")
    c_key = comp.get("key", "")
    widget_id = f"{prefix}_{c_key}" if c_key else f"{prefix}_{c_label}"

    if c_type == "input":
        st.session_state[c_key] = st.text_input(c_label, key=widget_id, value=st.session_state.get(c_key, ""))

    elif c_type == "button":
        if st.button(c_label, type="primary", key=widget_id):
            action = comp.get("action")
            if action:
                handle_action(action)

# LcmKernelService.java

def render_view(view_slug):
    """Fetches View definition from the Java backend and renders it."""

    st.info(f"Fetching view definition from UBOS Kernel: {view_slug}")

    # 1. 调用 Java API: /api/view/{slug} 获取视图 JSON
    view_data_response = call_ubos(f"{UBOS_API_URL}/view/{view_slug}", method="GET")

    # 2. 检查连接错误
    if isinstance(view_data_response, dict) and view_data_response.get("error_connection"):
        st.error(f"❌ Connection Error: {view_data_response['error_connection']}")
        return

    # 2. 提取数据和头信息
    view_response_data = view_data_response.get("data", {})
    headers = view_data_response.get("headers", {})

    # 3. 关键：获取分支信息
    resolved_branch = headers.get("X-UBOS-BRANCH", "UNKNOWN")

    # 4. 验证 View 数据结构并解包
    if 'content' in view_response_data:
        # (Unwrapping logic remains the same)
        try:
            view_data = json.loads(view_response_data['content'])
        except json.JSONDecodeError:
            st.error("❌ View content corrupted.")
            return

        if 'title' in view_data and 'components' in view_data:
            # --- DISPLAY THE BRANCH MARKER ---
            st.markdown(f"## 🌐 Branch: `{resolved_branch.upper()}`")
            st.markdown("---")

            st.subheader(view_data["title"])
            st.markdown("---")

            components = view_data.get("components", [])
            for comp in components:
                render_component(comp, prefix=view_slug)
            return

            return

    st.warning(f"View definition not found.")
    st.markdown("---")
    st.caption("Hint: Check if the 'VIEW' entity was committed with 'content' and 'title' fields.")
    st.text("Raw Backend Response:")
    st.json(view_data_response)

# ==========================================
# 4. Main Application Logic
# ==========================================

# Sidebar Mode Selection
mode = st.sidebar.radio("Select Mode", ["🤖 AI Architect (Dev)", "📱 App Browser (User)", "🕰️ Time-Travel Audit"])

# --- Mode A: AI Architect (Dev Mode) ---
if mode == "🤖 AI Architect (Dev)":
    st.header("🤖 AI Architect")
    st.caption("Generate logic, modify rules, and manage branches via natural language.")

    SYSTEM_PROMPT = """You are the Chief Architect of UBOS. Your task is to convert natural language requirements into UBOS Groovy script logic.
    [Example Commit]: kernel.commit("DATA_TYPE", "slug", "branch", jsonStr, "ai", "msg", null).block()
    """

    if "messages" not in st.session_state or not st.session_state.messages:
        st.session_state.messages = [{"role": "system", "content": SYSTEM_PROMPT}]

    if st.sidebar.button("🗑️ Clear Chat History"):
        st.session_state.messages = [{"role": "system", "content": SYSTEM_PROMPT}]
        st.experimental_rerun()

    for msg in st.session_state.messages:
        if isinstance(msg, dict) and msg.get("role") != "system":
            with st.chat_message(msg["role"]):
                content = msg.get("content")
                if content: st.write(content)
                elif "tool_calls" in msg: st.info("🔧 *AI is accessing Kernel Tools...*")

    if prompt := st.chat_input("Enter instructions..."):
        st.session_state.messages.append({"role": "user", "content": prompt})
        with st.chat_message("user"): st.write(prompt)
        with st.chat_message("assistant"):
            # tools = get_llm_tools() 
            # Note: get_llm_tools was not defined. Placeholder added below.
            st.warning("Tool execution logic is not implemented in this snippet.")
            
            # --- Place your working LLM Call here ---
            # NOTE: If using the original implementation logic, you must manually run
            
            st.error("LLM interaction code placeholder. Please ensure your working tool call logic is here.")
# --- Mode B: App Browser (User Mode) ---
elif mode == "📱 App Browser (User)":
    st.header("📱 Universal Business Browser")
    col1, col2 = st.columns([3,1])
    with col1: view_slug = st.text_input("Enter View ID", value="view.country.form")
    with col2:
        if st.button("Load View"): st.rerun()
    if view_slug: render_view(view_slug)

# --- Mode C: Audit Dashboard (The FIX is here) ---
elif mode == "🕰️ Time-Travel Audit":
    st.header("🕰️ Time-Travel Audit Dashboard")
    st.caption("Visualize process execution and data evolution.")

    # 1. Load Recent Processes (List)
    processes = get_processes()

    if isinstance(processes, dict) and processes.get("error_connection"):
        st.error(f"❌ API Connection Error: {processes['error_connection']}")
        processes = []

    if isinstance(processes, list) and len(processes) > 0:
        df_proc = pd.DataFrame(processes)
        try:
            df_proc['started_at'] = pd.to_datetime(df_proc['started_at']).dt.strftime('%Y-%m-%d %H:%M:%S')
        except:
            pass

        st.subheader("📋 Recent Business Processes")

        # --- Robust Selection using Selectbox ---
        process_names = df_proc['process_name'].tolist()
        selected_name = st.selectbox(
            "Select Process to Trace:",
            process_names,
            index=0,
            key='audit_process_select'
        )

        # Find the PID corresponding to the selected name
        selected_row = df_proc[df_proc['process_name'] == selected_name]
        pid = selected_row.iloc[0]["process_id"]
        pname = selected_row.iloc[0]["process_name"]

        st.divider()

        # 2. Load Details for the selected Process ID
        with st.spinner(f"Fetching details for {pname}..."):
            details = get_process_detail(pid)

        # ----------------------------------------
        # 【关键诊断块 - 强制打印 API 结果】
        st.markdown("### 🔍 Raw Backend Response (DEBUG)")
        st.json(details)
        st.markdown("---")
        # ----------------------------------------

        # 3. Render Timeline (The Visualization Fix)
        if isinstance(details, list) and len(details) > 0:
            # DEBUG print to confirm data receipt
            st.info(f"DEBUG: Successfully fetched {len(details)} commits for process ID: {pid}")

            # Render Loop (Guaranteed to show all items due to st.container)
            for i, step in enumerate(details):
                with st.container(border=True):
                    st.markdown(f"### ⚡ Step {i+1}: {step.get('entity_slug')} - {step.get('message')}")

                    cols = st.columns([1, 4])
                    with cols[0]:
                        st.markdown(f"**Type:** `{step.get('entity_type')}`")
                        st.caption(f"Author: {step.get('author_id')}")
                        st.caption(f"Time: {step.get('committed_at').split('.')[0]}")

                    with cols[1]:
                        st.markdown("#### Snapshot Data")
                        try:
                            st.json(json.loads(step.get('snapshot_data', '{}')))
                        except Exception:
                            st.text(step.get('snapshot_data', 'Malformed JSON'))
        else:
            st.warning("No linked data changes found for this process ID.")
    else:
        st.info("No audit records available. Please execute a business process first.")