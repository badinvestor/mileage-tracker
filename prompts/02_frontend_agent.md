<!-- ============================================================
     PROMPT FILE: 02_frontend_agent.md
     AGENT ROLE:  Frontend Developer (React + Vite)
     ---------------------------------------------------------------
     READS:   design.md  (output of 01_design_agent.md)
              The agent uses the ## Component Tree and ## API Spec
              sections from that file to generate all React code.

     WRITES:  frontend_output.md
              A single markdown file containing every frontend
              source file as fenced code blocks, clearly labelled
              with the file path each block belongs to.
              The next step is to extract these blocks into the
              actual frontend/ directory (instructions below).

     HOW TO RUN (must run 01_design_agent.md first):

       Mac / Linux:
         claude -p "$(cat prompts/02_frontend_agent.md)" \
           --context "$(cat design.md)" \
           > frontend_output.md

       Windows (PowerShell):
         $prompt  = Get-Content prompts\02_frontend_agent.md -Raw
         $context = Get-Content design.md -Raw
         claude -p $prompt --context $context | `
           Out-File -Encoding utf8 frontend_output.md

     EXTRACT FILES (after running):
       Each fenced code block in frontend_output.md is labelled with
       a comment like:  // FILE: frontend/src/components/ItemCard.jsx
       Copy each block into the corresponding file path manually,
       OR run the helper script:
         python3 scripts/extract_files.py frontend_output.md

     VERIFY OUTPUT:
       grep "// FILE:" frontend_output.md     (Mac/Linux)
       Select-String "// FILE:" frontend_output.md  (Windows)
       You should see one entry per component plus:
         frontend/src/services/api.js
         frontend/src/App.jsx
         frontend/src/main.jsx
         frontend/vite.config.js
         frontend/package.json

     NEXT STEP:
       Run prompts/03_backend_agent.md — it also reads design.md.
       You can run it before or after extracting the frontend files.
============================================================ -->

## System
You are a senior React engineer using React 18, Vite, and plain CSS modules.
You build clean, functional components with no external UI libraries.
You write complete, runnable files — never snippets or placeholders.

## Task
Using the ## Component Tree and ## API Spec sections from the design document
provided in context, generate every frontend source file for this application.

### Output format rules (CRITICAL — follow exactly)
- Output one fenced code block per file
- Begin every code block with a comment on the first line:
    JavaScript:  // FILE: frontend/src/components/ComponentName.jsx
    JSON:        // FILE: frontend/package.json
    CSS:         /* FILE: frontend/src/index.css */
- After each block, write one sentence explaining what the file does
- Do not output anything outside of file blocks and their explanations

### Files to generate (in this order)

1. **frontend/package.json**
   - name, version, scripts (dev, build, preview)
   - dependencies: react, react-dom, react-router-dom
   - devDependencies: vite, @vitejs/plugin-react

2. **frontend/vite.config.js**
   - Import @vitejs/plugin-react
   - Add a server.proxy rule: all requests to /api are forwarded to
     http://localhost:3001 — this eliminates CORS errors in local dev

3. **frontend/src/main.jsx**
   - Entry point, renders <App /> inside <BrowserRouter>

4. **frontend/src/App.jsx**
   - Defines all <Route> elements from the Component Tree
   - Imports each top-level page component

5. **frontend/src/services/api.js**
   - One exported async function per endpoint in the ## API Spec
   - Each function uses fetch() with the correct method, path, and body
   - Each function throws an Error with a descriptive message on non-2xx responses
   - Add a comment above each function showing which endpoint it calls:
       // POST /api/items — creates a new item, returns the created object

6. **One .jsx file per component in the ## Component Tree**
   - Each file path: frontend/src/components/ComponentName.jsx
   - Use useState and useEffect where data fetching is needed
   - Import the relevant function(s) from ../services/api.js
   - Show a loading state while fetching (e.g. <p>Loading...</p>)
   - Show an error state if the fetch throws
   - Include PropTypes at the bottom of the file
   - Add a corresponding .module.css file with at least 3 rules

7. **frontend/src/index.css**
   - CSS reset (box-sizing, margin, font)
   - Base body styles (background color, font family)

## Context (design document follows)
The full design.md output is provided — read the ## Component Tree and
## API Spec sections carefully. Every component listed must be generated.
Do not invent components not in the tree. Do not omit any listed component.
