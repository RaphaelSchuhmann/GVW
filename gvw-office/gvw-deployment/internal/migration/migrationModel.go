package migration

type Migration struct {
	id          string
	database    string
	field       string
	action      MigrationAction
	conditional ConditionalMigration
	value       string
}

type ConditionalMigration struct {
	condition   Branch
	trueBranch  Branch
	falseBranch Branch
	negated     bool
}

type Branch struct {
	field string
	value string
}

type MigrationAction string

const (
	Add         MigrationAction = "ADD"
	Delete      MigrationAction = "DELETE"
	Rename      MigrationAction = "RENAME"
	Conditional MigrationAction = "CONDITIONAL"
)
