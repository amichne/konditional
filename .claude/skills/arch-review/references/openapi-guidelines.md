# OpenAPI Specification Best Practices

## Schema Design

### Type Safety in Schemas

#### Avoid `anyOf` and `oneOf` When Possible
These make client code generation difficult and lose type safety:

```yaml
# Bad: Hard to generate type-safe clients
schema:
  oneOf:
    - type: string
    - type: integer

# Good: Use discriminated unions
schema:
  type: object
  required: [type, value]
  properties:
    type:
      type: string
      enum: [string, integer]
    value:
      oneOf:
        - type: string
        - type: integer
  discriminator:
    propertyName: type
    mapping:
      string: '#/components/schemas/StringValue'
      integer: '#/components/schemas/IntegerValue'
```

#### Use Enums for Closed Sets
```yaml
# Good: Compile-time safety
status:
  type: string
  enum: [pending, active, completed, failed]

# Bad: Any string accepted
status:
  type: string
```

#### Required vs Optional
Be explicit about required fields:

```yaml
# Good: Clear requirements
User:
  type: object
  required: [id, email]
  properties:
    id:
      type: string
      format: uuid
    email:
      type: string
      format: email
    phone:
      type: string  # Optional
```

### Nullability

#### Explicit Null Handling
```yaml
# Good: Explicit nullable
name:
  type: string
  nullable: true

# Bad: Implicit null handling
name:
  type: string
```

#### Avoid Nullable for Missing Data
```yaml
# Bad: null for different meanings
response:
  type: object
  properties:
    data:
      nullable: true  # null = not found OR error?

# Good: Explicit response types
response:
  oneOf:
    - $ref: '#/components/schemas/Success'
    - $ref: '#/components/schemas/NotFound'
    - $ref: '#/components/schemas/Error'
```

### Validation Constraints

#### Add Format Constraints
```yaml
email:
  type: string
  format: email

url:
  type: string
  format: uri

timestamp:
  type: string
  format: date-time

uuid:
  type: string
  format: uuid
  pattern: '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
```

#### Numeric Constraints
```yaml
age:
  type: integer
  minimum: 0
  maximum: 150

price:
  type: number
  multipleOf: 0.01
  minimum: 0

percentage:
  type: number
  minimum: 0
  maximum: 100
```

#### String Constraints
```yaml
username:
  type: string
  minLength: 3
  maxLength: 20
  pattern: '^[a-zA-Z0-9_-]+$'

description:
  type: string
  maxLength: 500
```

#### Array Constraints
```yaml
tags:
  type: array
  items:
    type: string
  minItems: 1
  maxItems: 10
  uniqueItems: true
```

## API Design Patterns

### Resource Naming

#### Use Plural Nouns
```yaml
# Good
/users
/orders
/products

# Bad
/user
/getUsers
/create-order
```

#### Hierarchical Resources
```yaml
# Good: Clear hierarchy
/users/{userId}/orders
/users/{userId}/orders/{orderId}

# Bad: Flat structure
/user-orders
/orders-by-user
```

### HTTP Methods

#### Standard CRUD Operations
```yaml
GET /users          # List users
POST /users         # Create user
GET /users/{id}     # Get specific user
PUT /users/{id}     # Replace user
PATCH /users/{id}   # Update user
DELETE /users/{id}  # Delete user
```

#### Non-CRUD Operations
Use POST for actions:
```yaml
POST /users/{id}/activate
POST /users/{id}/reset-password
POST /orders/{id}/cancel
```

### Response Codes

#### Success Codes
- `200 OK`: Successful GET, PUT, PATCH
- `201 Created`: Successful POST with resource creation
- `204 No Content`: Successful DELETE or POST with no response body
- `202 Accepted`: Async operation accepted

#### Client Error Codes
- `400 Bad Request`: Invalid request format/validation failure
- `401 Unauthorized`: Missing or invalid authentication
- `403 Forbidden`: Authenticated but not authorized
- `404 Not Found`: Resource doesn't exist
- `409 Conflict`: Resource conflict (e.g., duplicate)
- `422 Unprocessable Entity`: Semantic validation failure

#### Server Error Codes
- `500 Internal Server Error`: Unexpected server error
- `503 Service Unavailable`: Temporary unavailability

### Error Responses

#### Structured Error Format
```yaml
Error:
  type: object
  required: [code, message]
  properties:
    code:
      type: string
      description: Machine-readable error code
    message:
      type: string
      description: Human-readable error message
    details:
      type: array
      items:
        $ref: '#/components/schemas/ErrorDetail'
    timestamp:
      type: string
      format: date-time
    requestId:
      type: string
      format: uuid

ErrorDetail:
  type: object
  required: [field, error]
  properties:
    field:
      type: string
      description: Field that caused the error
    error:
      type: string
      description: Error message for this field
```

#### Example Error Response
```json
{
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "details": [
    {
      "field": "email",
      "error": "Invalid email format"
    },
    {
      "field": "age",
      "error": "Must be at least 18"
    }
  ],
  "timestamp": "2024-01-15T10:30:00Z",
  "requestId": "550e8400-e29b-41d4-a716-446655440000"
}
```

## Versioning Strategies

### URI Versioning
```yaml
/v1/users
/v2/users
```

Pros: Clear, easy to route
Cons: Multiple endpoints to maintain

### Header Versioning
```yaml
headers:
  - name: API-Version
    in: header
    required: true
    schema:
      type: string
      enum: [v1, v2]
```

Pros: Clean URIs
Cons: Less visible, harder to test

### Content Negotiation
```yaml
headers:
  - name: Accept
    in: header
    schema:
      type: string
      example: application/vnd.company.v2+json
```

## Pagination

### Cursor-Based Pagination (Recommended)
```yaml
parameters:
  - name: cursor
    in: query
    schema:
      type: string
  - name: limit
    in: query
    schema:
      type: integer
      minimum: 1
      maximum: 100
      default: 20

responses:
  '200':
    content:
      application/json:
        schema:
          type: object
          properties:
            data:
              type: array
              items:
                $ref: '#/components/schemas/User'
            nextCursor:
              type: string
              nullable: true
            hasMore:
              type: boolean
```

### Offset-Based Pagination
```yaml
parameters:
  - name: offset
    in: query
    schema:
      type: integer
      minimum: 0
      default: 0
  - name: limit
    in: query
    schema:
      type: integer
      minimum: 1
      maximum: 100
      default: 20

responses:
  '200':
    content:
      application/json:
        schema:
          type: object
          properties:
            data:
              type: array
            total:
              type: integer
            offset:
              type: integer
            limit:
              type: integer
```

## Filtering and Sorting

### Query Parameters for Filtering
```yaml
parameters:
  - name: status
    in: query
    schema:
      type: string
      enum: [active, inactive, pending]
  - name: createdAfter
    in: query
    schema:
      type: string
      format: date-time
  - name: minPrice
    in: query
    schema:
      type: number
```

### Sorting
```yaml
parameters:
  - name: sort
    in: query
    schema:
      type: string
      pattern: '^[+-]?(name|created|price)$'
      example: '-created'  # - for descending, + or none for ascending
```

## Security

### Authentication

#### Bearer Token (Recommended)
```yaml
components:
  securitySchemes:
    bearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT

security:
  - bearerAuth: []
```

#### API Key
```yaml
components:
  securitySchemes:
    apiKey:
      type: apiKey
      in: header
      name: X-API-Key

security:
  - apiKey: []
```

### Request/Response Examples

#### Include Examples
```yaml
User:
  type: object
  properties:
    id:
      type: string
      format: uuid
    email:
      type: string
      format: email
  example:
    id: "550e8400-e29b-41d4-a716-446655440000"
    email: "user@example.com"
```

## Documentation

### Description Fields
```yaml
paths:
  /users:
    get:
      summary: List users
      description: |
        Returns a paginated list of users. Results can be filtered
        by status and sorted by various fields.
        
        ## Filtering
        Use the `status` parameter to filter by user status.
        
        ## Sorting
        Use the `sort` parameter with field names prefixed by
        + (ascending) or - (descending).
      parameters:
        - name: status
          description: Filter users by status
```

### Operation IDs
```yaml
paths:
  /users:
    get:
      operationId: listUsers  # Used for code generation
    post:
      operationId: createUser
  /users/{id}:
    get:
      operationId: getUser
    patch:
      operationId: updateUser
    delete:
      operationId: deleteUser
```

## Backwards Compatibility

### Additive Changes (Safe)
- Adding optional properties
- Adding new endpoints
- Adding new enum values (if consumers use default case)
- Adding optional query parameters

### Breaking Changes (Unsafe)
- Removing properties
- Changing property types
- Making optional properties required
- Removing endpoints
- Changing response structure
- Removing enum values

### Deprecation Strategy
```yaml
paths:
  /old-endpoint:
    get:
      deprecated: true
      description: |
        **DEPRECATED**: Use `/new-endpoint` instead.
        This endpoint will be removed in v3.
```

## Common Anti-Patterns

### Over-Nesting
```yaml
# Bad: Deeply nested
User:
  properties:
    profile:
      properties:
        contact:
          properties:
            email:
              properties:
                value: string

# Good: Flatter structure
User:
  properties:
    email: string
```

### Ambiguous Naming
```yaml
# Bad: Unclear meaning
/data
/info
/stuff

# Good: Clear, specific
/users
/products
/orders
```

### Inconsistent Response Formats
```yaml
# Bad: Inconsistent
/users: { users: [...] }
/products: { data: [...] }
/orders: [...]

# Good: Consistent
/users: { data: [...] }
/products: { data: [...] }
/orders: { data: [...] }
```

### Missing Idempotency
```yaml
# Good: Idempotency key for POST
parameters:
  - name: Idempotency-Key
    in: header
    required: true
    schema:
      type: string
      format: uuid
```

## Performance Considerations

### Field Selection
```yaml
parameters:
  - name: fields
    in: query
    description: Comma-separated list of fields to include
    schema:
      type: string
      example: "id,name,email"
```

### Compression
```yaml
# Document in spec that compression is supported
headers:
  Accept-Encoding:
    description: Client supports gzip, deflate
    schema:
      type: string
      example: "gzip, deflate"
```

### Caching Headers
```yaml
responses:
  '200':
    headers:
      Cache-Control:
        schema:
          type: string
          example: "max-age=3600, must-revalidate"
      ETag:
        schema:
          type: string
```

## Observability Integration

### Correlation IDs
```yaml
parameters:
  - name: X-Request-ID
    in: header
    schema:
      type: string
      format: uuid
responses:
  headers:
    X-Request-ID:
      schema:
        type: string
        format: uuid
```

### Trace Context Propagation
```yaml
parameters:
  - name: traceparent
    in: header
    description: W3C Trace Context
    schema:
      type: string
      pattern: '^00-[0-9a-f]{32}-[0-9a-f]{16}-[0-9a-f]{2}$'
```
