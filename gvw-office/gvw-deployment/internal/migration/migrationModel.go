package migration

type Migration struct {
	Id          string               `json:"id"`
	Database    string               `json:"database"`
	Field       string               `json:"field"`
	Action      MigrationAction      `json:"action"`
	Conditional ConditionalMigration `json:"conditional"`
	Value       string               `json:"value"`
}

type ConditionalMigration struct {
	Condition   Branch `json:"condition"`
	TrueBranch  Branch `json:"trueBranch"`
	FalseBranch Branch `json:"falseBranch"`
	Negated     bool   `json:"negated"`
}

type Branch struct {
	Field string `json:"field"`
	Value string `json:"value"`
}

type MigrationAction string

const (
	MigrationAdd         MigrationAction = "ADD"
	MigrationDelete      MigrationAction = "DELETE"
	MigrationRename      MigrationAction = "RENAME"
	MigrationConditional MigrationAction = "CONDITIONAL"
)
