# Filter Evaluation Solution

This document explains the filter-evaluation solution that was added to the project.

It covers:

- the problem this solution solves,
- the data model used to represent nested filter logic,
- the operator/plugin architecture,
- the recursive evaluation algorithm,
- the REST API contract,
- examples of how expressions are interpreted,
- edge cases and current behavior,
- how to extend the solution with new operators.

---

## 1. Goal

The goal of this solution is to evaluate a nested filter configuration such as:

```yaml
config:
  - rule:
      and:
        - field: firstName
          op: EQUALS
          values:
            - john
        - field: lastName
          op: EQUALS
          values:
            - cleease
        - rule:
            or:
              - field: age
                op: GREATER
                values:
                  - "20"
              - field: age
                op: SMALLER
                values:
                  - "50"
```

against an input field map such as:

```json
{
  "firstName": "john",
  "lastName": "cleease",
  "age": "30"
}
```

and return a final boolean result.

In plain logic, the example means:

```text
(firstName == john) AND (lastName == cleease) AND ((age > 20) OR (age < 50))
```

The solution evaluates that structure dynamically using operator classes.

---

## 2. High-level architecture

The solution is split into 4 parts:

1. **Configuration model**
   - Represents the nested YAML/JSON structure in Java POJOs.

2. **Operator interface + implementations**
   - Each `op` value is mapped to a class implementing a common interface.

3. **Evaluation service**
   - Walks the rule tree recursively.
   - Delegates leaf-condition checks to the operator implementations.

4. **REST API endpoint**
   - Accepts a filter configuration and a field-value map.
   - Returns `{ "matches": true/false }`.

---

## 3. Files added for this solution

### Model / request / response

- `src/main/java/com/example/model/FilterConfiguration.java`
- `src/main/java/com/example/model/FilterEvaluationRequest.java`
- `src/main/java/com/example/model/FilterEvaluationResponse.java`

### Operator abstraction and implementations

- `src/main/java/com/example/service/filter/FieldFilterOperator.java`
- `src/main/java/com/example/service/filter/EqualsFieldFilterOperator.java`
- `src/main/java/com/example/service/filter/NotEqualFieldFilterOperator.java`
- `src/main/java/com/example/service/filter/GreaterFieldFilterOperator.java`
- `src/main/java/com/example/service/filter/SmallerFieldFilterOperator.java`

### Core evaluation service

- `src/main/java/com/example/service/FilterEvaluationService.java`

### REST endpoint

- `src/main/java/com/example/controller/BackendController.java`
  - endpoint: `POST /api/backend/filters/evaluate`

### Tests

- `src/test/java/com/example/model/FilterConfigurationTest.java`
- `src/test/java/com/example/service/FilterEvaluationServiceTest.java`
- updated: `src/test/java/com/example/controller/BackendControllerTest.java`

---

## 4. The filter configuration model

The configuration is represented by `FilterConfiguration`.

## Root structure

```java
FilterConfiguration
└── FiltersDefinition filters
    ├── List<FilterDependency> dependencies
    └── List<FilterConfigEntry> config
```

### `dependencies`
This section lists operator types used by the configuration, for example:

```yaml
dependencies:
  - type: EQUALS
  - type: GREATER
  - type: SMALLER
```

Right now, the evaluator does **not** enforce that every used operator must also exist in `dependencies`.
It uses the actual `op` values found in the rule nodes.

So at the moment:
- `dependencies` is descriptive metadata,
- not a strict validation source.

That can be changed later if needed.

### `config`
Each config entry contains a top-level rule:

```yaml
config:
  - rule:
      and:
        ...
```

A configuration can contain multiple top-level config entries.
The evaluator currently treats them as:

```text
config[0] AND config[1] AND config[2] ...
```

because all config entries must match.

---

## 5. Rule model

The recursive rule structure is modeled like this:

```java
FilterConfigEntry
└── FilterRuleGroup rule
    ├── List<FilterRuleNode> and
    └── List<FilterRuleNode> or
```

A `FilterRuleNode` can be one of two things:

### A leaf condition
Example:

```yaml
- field: age
  op: GREATER
  values:
    - "20"
```

Mapped as:

- `field = "age"`
- `op = "GREATER"`
- `values = ["20"]`
- `rule = null`

### A nested rule group
Example:

```yaml
- rule:
    or:
      - field: age
        op: GREATER
        values:
          - "20"
      - field: age
        op: SMALLER
        values:
          - "50"
```

Mapped as:

- `field = null`
- `op = null`
- `values = null`
- `rule = FilterRuleGroup(...)`

This is what allows recursion.

---

## 6. Operator interface

The operator abstraction is:

```java
public interface FieldFilterOperator {
    Set<String> supportedOperators();
    boolean matches(String inputField, List<String> values);
}
```

This design is important because it decouples:

- **how a rule is walked**
from
- **how a specific operator behaves**

So the evaluator does not contain hardcoded logic like:

```java
if (op.equals("EQUALS")) ...
if (op.equals("GREATER")) ...
```

Instead, it looks up the correct operator implementation and delegates evaluation.

---

## 7. Supported operators

## `EqualsFieldFilterOperator`
Supports aliases like:

- `EQUALS`
- `EQUAL`
- `EQ`
- `equals`
- `F-2`

Behavior:

```text
true if any configured value equals the input field value (case-insensitive)
```

Example:

```text
inputField = "john"
values = ["john", "jane"]
=> true
```

---

## `NotEqualFieldFilterOperator`
Supports aliases like:

- `NOT_EQUAL`
- `NOT EQUAL`
- `NEQ`
- `not equal`
- `not_equal`

Behavior:

```text
true if none of the configured values equals the input field value
```

Example:

```text
inputField = "john"
values = ["mike", "anna"]
=> true
```

---

## `GreaterFieldFilterOperator`
Supports aliases like:

- `GREATER`
- `GT`
- `greater`

Behavior:

- parses the input field as a number,
- parses each configured value as a number,
- returns true if:

```text
inputField > any configured value
```

Example:

```text
inputField = "30"
values = ["20"]
=> true
```

Implementation uses `BigDecimal`, so numeric comparison is consistent for integer and decimal values.

---

## `SmallerFieldFilterOperator`
Supports aliases like:

- `SMALLER`
- `LT`
- `smaller`

Behavior:

- parses the input field as a number,
- parses each configured value as a number,
- returns true if:

```text
inputField < any configured value
```

Example:

```text
inputField = "30"
values = ["50"]
=> true
```

---

## 8. Operator lookup strategy

`FilterEvaluationService` receives all operator beans and builds a lookup map.

Conceptually:

```java
alias -> operator implementation
```

Examples:

```text
EQUALS     -> EqualsFieldFilterOperator
F_2        -> EqualsFieldFilterOperator
NOT_EQUAL  -> NotEqualFieldFilterOperator
GREATER    -> GreaterFieldFilterOperator
SMALLER    -> SmallerFieldFilterOperator
```

### Normalization
Operator names are normalized before lookup:

- trim spaces
- uppercase
- replace `-` with `_`
- replace spaces with `_`

So these are treated equivalently:

- `NOT_EQUAL`
- `not_equal`
- `NOT EQUAL`
- `not equal`

This makes the API more tolerant and easier to evolve.

---

## 9. Evaluation algorithm

The central method is:

```java
boolean evaluate(FilterConfiguration configuration, Map<String, String> inputFields)
```

## Step 1: validate request

The service validates:

- configuration is not null
- `filters` exists
- `config` is not empty
- `inputFields` is not empty

If these are invalid, it throws `IllegalArgumentException`.

---

## Step 2: evaluate all top-level config entries

Each top-level `config` entry is evaluated.
All of them must match.

That means the top-level semantics are:

```text
config[0] AND config[1] AND ...
```

---

## Step 3: evaluate a rule group

A rule group may contain:

- `and`
- `or`
- or both

Behavior is:

```text
(andResult) AND (orResult)
```

Where:

- `andResult = true` if all `and` nodes match
- `orResult = true` if any `or` node matches

If only one side exists, the missing side is treated as `true`.

So:

- only `and` => all must match
- only `or` => at least one must match
- both => both conditions must hold

---

## Step 4: evaluate a rule node

A `FilterRuleNode` is evaluated in one of two ways.

### If it contains a nested rule group
Then evaluation is recursive:

```java
return evaluateRuleGroup(node.getRule(), inputFields);
```

### If it is a leaf condition
Then the service:

1. reads `field`
2. reads `op`
3. reads `values`
4. looks up the actual field value from `inputFields`
5. resolves the correct operator implementation
6. runs `operator.matches(actualValue, values)`

---

## 10. Example walkthrough

Given this configuration:

```yaml
config:
  - rule:
      and:
        - field: firstName
          op: EQUALS
          values:
            - john
        - field: lastName
          op: EQUALS
          values:
            - cleease
        - rule:
            or:
              - field: age
                op: GREATER
                values:
                  - "20"
              - field: age
                op: SMALLER
                values:
                  - "50"
```

And input:

```json
{
  "firstName": "john",
  "lastName": "cleease",
  "age": "30"
}
```

Evaluation:

1. `firstName EQUALS [john]`
   - `john == john` => `true`

2. `lastName EQUALS [cleease]`
   - `cleease == cleease` => `true`

3. nested `or`
   - `age GREATER [20]`
     - `30 > 20` => `true`
   - `age SMALLER [50]`
     - `30 < 50` => `true`
   - OR result => `true`

4. final `and`
   - `true AND true AND true`
   - final result => `true`

---

## 11. REST API

Endpoint added to `BackendController`:

```text
POST /api/backend/filters/evaluate
```

Consumes:

```text
application/json
```

Produces:

```text
application/json
```

### Request shape

```json
{
  "configuration": {
    "filters": {
      "config": [
        {
          "rule": {
            "and": [
              {
                "field": "firstName",
                "op": "EQUALS",
                "values": ["john"]
              }
            ]
          }
        }
      ]
    }
  },
  "inputFields": {
    "firstName": "john"
  }
}
```

### Success response

```json
{
  "matches": true
}
```

or

```json
{
  "matches": false
}
```

### Invalid request behavior
If the request is malformed or contains an unsupported operator, the endpoint returns HTTP 400.

Current response body on bad input is:

```json
{
  "matches": false
}
```

That is intentionally simple, but can be expanded later to return error details.

---

## 12. Current behavior and edge cases

## Missing field in `inputFields`
If a rule references a field that is not present in `inputFields`, that leaf condition returns `false`.

Example:

```text
rule field = age
inputFields does not contain age
=> false
```

---

## Empty values list
Rejected as invalid.

---

## Unknown operator
Rejected as invalid.

Example:

```text
Unsupported filter operator: UNKNOWN
```

---

## Numeric operators on non-numeric values
`GREATER` and `SMALLER` require numeric values.
If the field or configured values are not numeric, an `IllegalArgumentException` is thrown.

---

## Case sensitivity
String comparisons in `EQUALS` / `NOT_EQUAL` are currently **case-insensitive**.

Example:

```text
john == JOHN
=> true
```

If strict case-sensitive behavior is needed, that can be changed in the operator implementations.

---

## `dependencies` are not enforced yet
The solution currently does **not** verify that every `op` used in the rules also appears in:

```yaml
filters.dependencies
```

That means the following is currently allowed:

- config uses `EQUALS`
- dependencies omit `EQUALS`
- evaluation still succeeds if an operator implementation exists

If you want stricter behavior, the evaluator can add a validation step.

---

## 13. Why this design is good

This solution is intentionally extensible.

### Benefits

#### 1. Open for extension
To add a new operator, you do not need to rewrite the evaluator.
You only add another class implementing `FieldFilterOperator`.

#### 2. Recursive model supports complex logic
Because a rule node can contain another rule group, the solution naturally supports nested boolean logic.

#### 3. Clear separation of concerns
- POJOs represent configuration shape
- operator classes implement comparison behavior
- service performs traversal/evaluation
- controller exposes HTTP API

#### 4. Spring-friendly
Operators are Spring components, so they are auto-discovered and injected.

---

## 14. How to add a new operator later

Suppose you want to add `CONTAINS`.

You would create:

```java
@Component
public class ContainsFieldFilterOperator implements FieldFilterOperator {

    @Override
    public Set<String> supportedOperators() {
        return Set.of("CONTAINS");
    }

    @Override
    public boolean matches(String inputField, List<String> values) {
        return values.stream().anyMatch(value ->
            inputField != null && inputField.toLowerCase().contains(value.toLowerCase())
        );
    }
}
```

That is all.

No change is required in the recursion logic.

---

## 15. Tests added

### `FilterConfigurationTest`
Verifies that the YAML structure deserializes correctly into the POJOs.

### `FilterEvaluationServiceTest`
Verifies:

- nested `and/or` behavior,
- false result when an `and` condition fails,
- invalid operator rejection.

### `BackendControllerTest`
Verifies the new REST endpoint returns:

```json
{ "matches": true }
```

for a valid request.

---

## 16. Possible next improvements

Several useful follow-ups are possible.

### A. Enforce `dependencies`
Validate that every operator used in rules exists in `filters.dependencies`.

### B. Better error response
Instead of returning only:

```json
{ "matches": false }
```

on invalid input, return a richer error payload like:

```json
{
  "matches": false,
  "error": "Unsupported filter operator: XYZ"
}
```

### C. Add more operators
Examples:

- `IN`
- `CONTAINS`
- `STARTS_WITH`
- `ENDS_WITH`
- `LESS_OR_EQUAL`
- `GREATER_OR_EQUAL`
- regex matching

### D. Support typed field values
Right now all incoming values are `String`.
Later, input can be changed to `Map<String, Object>` and operators can handle richer types.

### E. Pre-validation endpoint
Add a separate endpoint that validates filter configuration structure without evaluating it.

---

## 17. Summary

This solution adds a complete framework for evaluating nested filter configurations.

It provides:

- a recursive POJO model for nested rules,
- an operator interface,
- pluggable operator classes,
- recursive AND/OR evaluation,
- a backend REST API that returns a boolean result.

It correctly supports logic like:

```text
(firstName equals [john] AND lastName equals [cleease] AND (age greater [20] OR age smaller [50]))
```

and is structured to be extended safely over time.

