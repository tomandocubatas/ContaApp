<!-- agents-template-version: 6 -->
<!--
  Plantilla MÍNIMA de instrucciones para que un agente (Gemini/Antigravity, Claude Code, Codex)
  use Agent MemoryBank Hub como memoria persistente del proyecto.

  El protocolo completo ya NO se copia en cada proyecto: lo sirve el propio MCP del Hub
  (tool get_protocol, recurso memorybank://protocol, y las "instructions" que el servidor envía
  al conectarse). Fuente única: apps/mcp-server/protocol/PROTOCOL.md en el repo del Hub.
  Cambiar el protocolo = editar ese fichero y redesplegar; ningún proyecto se queda atrás.

  Cómo usarla:
  1. Copia este fichero a la raíz de tu proyecto como AGENTS.md (lo leen de forma nativa Codex y
     Google Antigravity; si ya tienes un GEMINI.md, ese tiene prioridad en Antigravity, así que no
     crees ambos). Para Claude Code, añade una línea `@AGENTS.md` en tu CLAUDE.md.
  2. Sustituye <PROJECT_KEY> por la clave real del proyecto (mayúsculas, sin espacios).
  3. Crea el proyecto una vez con create_project (ver get_protocol({ section: "0" })) o desde el
     dashboard de producción http://192.168.2.155:9468. En ese mismo portal, "Guía para agentes"
     (http://192.168.2.155:9468/guia) te da este fichero con la clave del proyecto ya puesta.
  4. Comprueba que tu .mcp.json (o la config global del cliente) apunta a
     http://192.168.2.155:9468/mcp.
  5. Añade debajo de "Reglas propias de este proyecto" solo lo específico del proyecto
     (compilación, versionado, despliegue...). No dupliques aquí secciones del protocolo.
-->

# Memoria de proyecto — Agent MemoryBank Hub

Este proyecto usa **dos memorias, siempre las dos**: `agent-memorybank` (Hub,
`projectKey: CONTAPP`) para la verdad del **proyecto** y `codebase-memory-mcp` para la
verdad del **código**. Si el código contradice al Hub, manda el código: corrige el Hub.

## Protocolo

El protocolo operativo completo lo sirve el Hub y es la **fuente única**: manda sobre cualquier
copia antigua que encuentres en este repo.

```
get_protocol({ projectKey: "CONTAPP" })                    // completo, al empezar la sesión
get_protocol({ projectKey: "CONTAPP", section: "index" })  // índice de secciones
get_protocol({ projectKey: "CONTAPP", section: "12" })     // una sección ("1", "1.1", "nunca"...)
```

Resumen mínimo, por si todavía no lo has cargado — **no se toca código sin esto**:

1. `get_protocol` y el **arranque de su sección 1**: es proporcional al trabajo (hay un mínimo que
   nunca se omite y consultas que dependen de lo que vayas a hacer; ante la duda, el completo). Lo que
   hay que consultar lo define el protocolo, no este fichero.
2. Historia y tarea en el Hub **y la tarea en `IN_PROGRESS` antes de editar nada** —código, protocolo,
   `AGENTS.md`, documentación o configuración, también en un cambio de una línea— (sin duplicar una tarea
   abierta). Crearla no basta: ponla en curso con `update_task_status` y `agentName` antes del primer cambio.
   Cada tarea lleva `effort` y al menos un componente de arquitectura; los puntos y la talla los estimas tú.
3. `agentName`/`assignedAgent` en **cada** escritura al Hub.
4. Para cerrar una tarea: evidencia `PASS` (`add_task_evidence`) y el bloque de cierre verificable
   de la sección 11.
5. Si el Hub no responde o falta una tool que debería existir: **detente y dilo**. No trabajes a
   ciegas. (Si lo que no conecta es `codebase-memory-mcp`, no pares: díselo al usuario y sigue con
   Read/Grep, ver sección 1 del protocolo.)
6. Compara el `agents-template-version` de este fichero (un comentario HTML, normalmente al
   principio) con el que indica la cabecera de `get_protocol`; si es menor o falta, avisa al usuario de que este `AGENTS.md` está
   desfasado (no lo reescribas tú).
7. En `codebase-memory-mcp` empieza por `list_projects` y pasa **siempre** `project` con el nombre
   exacto que devuelve (en mayúsculas, p. ej. `MYSUBS`). No indexes con `index_repository` y una ruta
   de tu equipo: los repos ya viven en el servidor, usa `sync_repo`. Detalle en la sección 1 del protocolo.
8. **Subagentes.** Al empezar, desde la raíz del repo, deja al día los subagentes que el Hub asigna a este
   proyecto (se gestionan en el portal), sin instalar nada (solo `curl` y `sh`):
   `curl -fsSL "http://192.168.2.155:9468/api/guides/agents?format=sh&target=claude-code&projectKey=CONTAPP" | sh`.
   Donde `target` es tu herramienta (hoy `claude-code`). No toca los agentes propios del repo. Si el Hub no
   responde, sigue con los que ya hay commiteados. No hagas commit de lo que cambie sin que te lo pidan.

## Reglas propias de este proyecto

- **Dependencia de API.** ContaApp es un cliente Android de solo lectura de la API de ContMovApp
  (Hub `projectKey: CONTMOVAPP`, `codebase-memory-mcp` `project: CONTMOVAPP`). Antes de tocar
  rutas, modelos o DTO de red, consulta la API real en el grafo de CONTMOVAPP: manda el backend.
- **`codebase-memory-mcp`:** esta app esta indexada como `project: CONTAAPP` (rama
  `codex/contaapp-baseline`), distinto de la clave del Hub (`CONTAPP`). El grafo refleja el
  ultimo commit sincronizado: lo no commiteado se lee del fichero.
