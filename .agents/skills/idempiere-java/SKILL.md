---
name: idempiere-java
description: Use this when working on iDempiere Java code such as SvrProcess classes, database access with DB utilities, X_ models, ZK UI forms, attachments, Apache POI Excel handling, and JasperReports in an iDempiere plugin. Do not use for generic Java tasks outside iDempiere.
---

# iDempiere Java Skill

## Goal

Produce production-ready iDempiere Java code that follows framework conventions, uses existing utilities first, and stays safe for transactions, performance, and maintainability.

## Default assumptions

Assume all of the following unless the repository clearly shows otherwise:

- iDempiere plugin codebase
- Java 8+ or newer
- PostgreSQL
- Generated `X_` model classes available
- Framework utilities such as `DB`, `Query`, `Env`, `Trx`, `MAttachment`
- ZK-based UI for forms/windows
- JasperReports for reporting
- Apache POI for Excel work

## Core coding rules

### 1. Processes (`SvrProcess`)

When creating or fixing a process:

- Extend `SvrProcess`
- Define the Process using @org.adempiere.base.annotation.Process
    EG @org.adempiere.base.annotation.Process(
		name = "za.co.ntier.wsp_atr.process.GenerateWspAtrTablesFromTemplate")
		public class GenerateWspAtrTablesFromTemplate extends SvrProcess {...
- Read parameters in using @Parameter
    EG @Parameter(name = "FileName")
	   private String filePath;
- Put business logic in `doIt()`
- Validate required parameters early
- Use `log.info(...)` or appropriate logging for key steps
- Use transaction-aware framework utilities
- Return a clear result message

Checklist:

- Parameters parsed correctly
- Required values validated before processing
- Logging added at important checkpoints
- Exceptions fail clearly and early
- Transaction handling is correct

### 2. Transactions

- Respect `trxName`
- Do not ignore transaction context
- Use `Trx.get(trxName, true)` where explicit transaction handling is needed
- Batch commits for large operations, for example every 1000 rows
- Roll back on failure when needed

Preferred pattern:

- Keep one logical transaction per operation
- Commit in batches only for large imports or generated rows
- Avoid partial silent failures

### 3. Models

- Prefer generated `X_` classes and model APIs
- Prefer setters plus `saveEx()`
- Avoid raw SQL inserts when a model save is appropriate
- Use `Query` where it fits better than manual SQL
- Keep table and column naming consistent with iDempiere conventions

### 4. Database access

- Prefer iDempiere utilities over plain JDBC
- Use `DB.getSQLValueEx`, `DB.getSQLArrayObjectsEx`, `DB.executeUpdateEx`, and `Query` as appropriate
- Always pass parameters safely
- Always use `trxName` where applicable
- Avoid hardcoded IDs

### 5. ZK UI

When working on forms or UI logic:

- Use standard ZK/iDempiere components such as `Window`, `Vbox`, `Hbox`, `Listbox`, `Label`, `Button`
- Use `Events.ON_CLICK` and normal event listeners
- Keep UI responsive
- Avoid heavy blocking work directly in the UI thread when possible
- Keep layout and naming consistent with surrounding code

### 6. Attachments

- Use `MAttachment`
- Verify attachment integrity after writing when file correctness matters
- Be careful with memory when handling large files
- Preserve original file names and content types where useful
- Fail clearly if an attachment is empty or corrupted

### 7. Excel (`Apache POI`)

- Use `WorkbookFactory.create(...)`
- Handle encrypted files safely
- Prefer stream-based handling where practical
- Be careful about memory spikes on large workbooks
- Validate generated workbooks before attaching or returning them
- Keep workbook, stream, and resource cleanup correct

### 8. JasperReports

- Keep pixel-based sizing/layout consistent across reports
- Maintain standard page width, margins, and section spacing used by the project
- Use subreports for separate sections when appropriate
- Pass parameters clearly and consistently
- Keep styles centralized and consistent

## Workflow for solving tasks

When solving a problem in this repo:

1. Identify the task type:
   - Process
   - UI
   - Import
   - Report

2. Apply iDempiere patterns first, not generic Java patterns.

3. Prefer:
   - Existing framework classes
   - Existing project conventions
   - Consistent DB table and column naming

4. Ensure:
   - Transaction safety
   - Performance for large datasets
   - Maintainability
   - Clear logging
   - Minimal surprise for other iDempiere developers

## Guardrails

- Do not use plain JDBC when an iDempiere DB utility already fits
- Do not ignore `trxName`
- Do not hardcode record IDs.  If needed use record UUIDs instead.
- Do not bypass model validation unless there is a clear reason
- Do not choose generic Java patterns over established iDempiere conventions
- Always consider batching, caching, and query cost for large data volumes

## Preferred response style

When asked to generate code:

- Produce complete, runnable class or method updates when possible
- Match the repo’s existing naming and formatting
- Keep imports realistic
- Explain framework-specific decisions briefly
- Prefer exact code over abstract advice

## Trigger examples

Use this skill for prompts such as:

- "Create iDempiere process"
- "Fix DB.getSQLArrayObjectsEx error"
- "Build Jasper report section"
- "Add ZK form upload button"
- "Excel import validation in iDempiere"
- "Create X_ model save logic"
- "Fix transaction handling in SvrProcess"