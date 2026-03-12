# Task 1.2: Proto Definitions — DAP gRPC Contracts

**Sprint:** 1 — Foundation
**Points:** 5
**Status:** TODO

## Files

- Create: `dts-stack/proto/buf.yaml`
- Create: `dts-stack/proto/buf.gen.yaml`
- Create: `dts-stack/proto/dap/v1/common.proto`
- Create: `dts-stack/proto/dap/v1/ontology.proto`
- Create: `dts-stack/proto/dap/v1/skill.proto`
- Create: `dts-stack/proto/dap/v1/intent.proto`
- Create: `dts-stack/proto/dap/v1/security.proto`
- Create: `dts-stack/proto/dap/v1/audit.proto`
- Create: `dts-stack/proto/dap/v1/platform.proto`

## Step 1: Create common.proto

```protobuf
syntax = "proto3";
package dap.v1;
option java_package = "com.dts.proto.dap.v1";
option go_package = "github.com/billyhotjava/dts/proto/dap/v1";

message RequestContext {
  string user_id = 1;
  string tenant_id = 2;
  string trace_id = 3;
  string jwt_token = 4;
  repeated string permissions = 5;
}

message PageRequest {
  int32 page = 1;
  int32 size = 2;
  string sort_by = 3;
  bool ascending = 4;
}

message PageResponse {
  int32 total = 1;
  int32 page = 2;
  int32 size = 3;
}
```

## Step 2: Create ontology.proto

```protobuf
syntax = "proto3";
package dap.v1;
import "dap/v1/common.proto";
import "google/protobuf/struct.proto";

service OntologyService {
  rpc CreateObjectType(CreateObjectTypeRequest) returns (ObjectTypeResponse);
  rpc GetObjectType(GetObjectTypeRequest) returns (ObjectTypeResponse);
  rpc ListObjectTypes(ListObjectTypesRequest) returns (ListObjectTypesResponse);
  rpc UpdateObjectType(UpdateObjectTypeRequest) returns (ObjectTypeResponse);
  rpc DeleteObjectType(DeleteObjectTypeRequest) returns (DeleteObjectTypeResponse);
  rpc CreateInstance(CreateInstanceRequest) returns (InstanceResponse);
  rpc GetInstance(GetInstanceRequest) returns (InstanceResponse);
  rpc QueryInstances(QueryInstancesRequest) returns (QueryInstancesResponse);
  rpc CreateRelationship(CreateRelationshipRequest) returns (RelationshipResponse);
  rpc GetRelationships(GetRelationshipsRequest) returns (GetRelationshipsResponse);
}

message ObjectType {
  string name = 1;
  string namespace = 2;
  string description = 3;
  repeated PropertyDef properties = 4;
  repeated ActionDef actions = 5;
}

message PropertyDef {
  string name = 1;
  string type = 2;
  bool required = 3;
  string classification = 4;
  repeated string enum_values = 5;
  string source = 6;
  string unit = 7;
}

message ActionDef {
  string name = 1;
  string risk_level = 2;
  string description = 3;
}
```

## Step 3: Create skill.proto

```protobuf
syntax = "proto3";
package dap.v1;
import "dap/v1/common.proto";
import "google/protobuf/struct.proto";

service SkillService {
  rpc Execute(SkillRequest) returns (stream SkillResponse);
  rpc Describe(DescribeRequest) returns (SkillDescriptor);
  rpc Validate(ValidateRequest) returns (ValidateResponse);
}

message SkillRequest {
  string skill_name = 1;
  string trace_id = 2;
  RequestContext context = 3;
  google.protobuf.Struct input = 4;
}

message SkillResponse {
  enum Status { RUNNING = 0; COMPLETED = 1; FAILED = 2; AWAITING_APPROVAL = 3; }
  Status status = 1;
  string step_description = 2;
  google.protobuf.Struct output = 3;
  AuditEntry audit = 4;
}

message AuditEntry {
  string event_type = 1;
  string source = 2;
  repeated string data_accessed = 3;
  int64 duration_ms = 4;
  int32 token_cost = 5;
  string risk_level = 6;
}

message SkillDescriptor {
  string name = 1;
  string namespace = 2;
  string description = 3;
  string type = 4;
  string risk_level = 5;
  repeated string permissions = 6;
  google.protobuf.Struct input_schema = 7;
  google.protobuf.Struct output_schema = 8;
}
```

## Step 4: Create intent.proto, security.proto, audit.proto, platform.proto

(See implementation plan for full definitions)

## Step 5: Create buf.yaml and buf.gen.yaml

```yaml
# buf.yaml
version: v2
modules:
  - path: .
lint:
  use:
    - DEFAULT
breaking:
  use:
    - FILE
```

## Step 6: Verify

Run: `cd dts-stack/proto && buf lint`
Expected: No errors

Run: `buf generate`
Expected: Java + Python stubs generated

## Step 7: Commit

```bash
git add dts-stack/proto/
git commit -m "feat(s1): define DAP v1 proto contracts"
```
