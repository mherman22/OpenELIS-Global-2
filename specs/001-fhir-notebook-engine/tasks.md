# FHIR-Native Generic Notebook Engine — M1 Tasks

## Milestone M1: FHIR Persistence Layer

| ID   | Description                                                              | Status |
| ---- | ------------------------------------------------------------------------ | ------ |
| T001 | Create branch `feat/001-fhir-notebook-engine-m1-fhir-persistence`        | [x]    |
| T002 | Liquibase changeset: add `questionnaire_uuid` column to `notebook` table | [x]    |
| T003 | ORM validation test: `NoteBookOrmTest` — verify `@Column` mapping        | [x]    |
| T004 | Add `questionnaireUuid` field + getter/setter to `NoteBook` entity       | [x]    |
| T005 | Unit test: `shouldUpsertQuestionnaireByUrl`                              | [x]    |
| T006 | Unit test: `shouldNotCreateDuplicateOnSecondUpload`                      | [x]    |
| T007 | Unit test: `shouldSaveInProgressResponse`                                | [x]    |
| T008 | Unit test: `shouldReturnLatestInProgressDraft`                           | [x]    |
| T009 | Unit test: `shouldReturnEmptyWhenNoDraftExists`                          | [x]    |
| T010 | Create `NotebookFhirPersistenceService` interface                        | [x]    |
| T011 | Impl: `saveOrUpdateQuestionnaire` (upsert by URL)                        | [x]    |
| T012 | Impl: `saveQuestionnaireResponse`                                        | [x]    |
| T013 | Impl: `getInProgressResponse`                                            | [x]    |
| T014 | Impl: `getLatestQuestionnaire`                                           | [x]    |
| T015 | Extend `NotebookTemplateConfigurationHandler`: build + save FHIR Q       | [x]    |
| T016 | Controller test: `getQuestionnaire` returns 200                          | [x]    |
| T017 | Controller test: `getQuestionnaire` returns 404                          | [x]    |
| T018 | Controller test: `saveQuestionnaireResponse` returns 200                 | [x]    |
| T019 | Controller test: `getInProgressResponse` returns 404 when empty          | [x]    |
| T020 | Create `NotebookFhirResponseForm`                                        | [x]    |
| T021 | Controller endpoint: GET `/{notebookId}/questionnaire`                   | [x]    |
| T022 | Controller endpoint: POST `/{notebookId}/response`                       | [x]    |
| T023 | Controller endpoint: GET `/{notebookId}/response/in-progress`            | [x]    |
| T024 | Pre-commit formatting + open PR against `develop`                        | [ ]    |
