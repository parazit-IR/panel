# Renewal Traffic Application

Traffic policies are applied as absolute target values.

```mermaid
flowchart TD
  A[Traffic Policy] --> B{Policy}
  B -->|RESET_TO_PLAN_LIMIT| C[Set plan limit and reset usage]
  B -->|ADD_TO_REMAINING| D[Remaining plus plan limit and reset usage]
  B -->|ADD_TO_TOTAL_LIMIT| E[Current total plus plan limit, preserve usage]
  B -->|UNCHANGED| F[Keep current limit and usage]
```

The current 3x-ui mapping treats `0` total traffic as unlimited. A renewal plan with no traffic limit is also applied as unlimited.
