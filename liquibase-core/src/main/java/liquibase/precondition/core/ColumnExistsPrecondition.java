package liquibase.precondition.core;

import static java.lang.String.format;

import java.sql.SQLException;
import java.sql.Statement;

import liquibase.changelog.DatabaseChangeLog;
import liquibase.changelog.ChangeSet;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.precondition.AbstractPrecondition;
import liquibase.snapshot.SnapshotGeneratorFactory;
import liquibase.structure.ObjectName;
import liquibase.structure.core.Column;
import liquibase.structure.core.Schema;
import liquibase.exception.*;
import liquibase.precondition.Precondition;
import liquibase.structure.core.Table;
import liquibase.util.JdbcUtils;
import liquibase.util.StringUtils;

public class ColumnExistsPrecondition extends AbstractPrecondition {
    private String catalogName;
    private String schemaName;
    private String tableName;
    private String columnName;

    @Override
    public String getSerializedObjectNamespace() {
        return STANDARD_CHANGELOG_NAMESPACE;
    }

    public String getCatalogName() {
        return catalogName;
    }

    public void setCatalogName(String catalogName) {
        this.catalogName = catalogName;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    @Override
    public Warnings warn(Database database) {
        return new Warnings();
    }

    @Override
    public ValidationErrors validate(Database database) {
        return new ValidationErrors();
    }

    @Override
    public void check(Database database, DatabaseChangeLog changeLog, ChangeSet changeSet) throws PreconditionFailedException, PreconditionErrorException {
		if (canCheckFast(database)) {
			checkFast(database, changeLog);

		} else {
			checkUsingSnapshot(database, changeLog, changeSet);
		}
	}

    private void checkUsingSnapshot(Database database, DatabaseChangeLog changeLog, ChangeSet changeSet) throws PreconditionFailedException, PreconditionErrorException {
        Column example = new Column();
        if (StringUtils.trimToNull(getTableName()) != null) {
            example.relation = new Table(database.correctObjectName(getTableName(), Table.class)).setSchema(new Schema(getCatalogName(), getSchemaName()));
        }
        example.setName(database.correctObjectName(getColumnName(), Column.class));

        try {
            if (!SnapshotGeneratorFactory.getInstance().has(example, database)) {
                throw new PreconditionFailedException("Column '" + new ObjectName(catalogName, schemaName, getTableName(), getColumnName()) + "' does not exist", changeLog, this);
            }
        } catch (LiquibaseException e) {
            throw new PreconditionErrorException(e, changeLog, this);
        }
    }

	private boolean canCheckFast(Database database) {
		if (getCatalogName() != null)
			return false;

		if (!(database.getConnection() instanceof JdbcConnection))
			return false;

		if (getColumnName() == null)
			return false;

		if (!getColumnName().matches("(?i)[a-z][a-z_0-9]*"))
			return false;

		if (!(getSchemaName() != null || database.getDefaultSchemaName() != null)) {
			return false;
		}

		return true;
	}

	private void checkFast(Database database, DatabaseChangeLog changeLog)
			throws PreconditionFailedException, PreconditionErrorException {

		Statement statement = null;
		try {
			statement = ((JdbcConnection) database.getConnection())
					.createStatement();

			String schemaName = getSchemaName();
			if (schemaName == null) {
				schemaName = database.getDefaultSchemaName();
			}
			String tableName = getTableName();
			String columnName = getColumnName();

			try {
				String sql = format("select t.%s from %s.%s t where 0=1",
						columnName, schemaName, tableName);
				statement.executeQuery(sql).close();

				// column exists
				return;

			} catch (SQLException e) {
				// column or table does not exist
				throw new PreconditionFailedException(format(
						"Column %s.%s.%s does not exist", schemaName,
						tableName, columnName), changeLog, this);
			}

		} catch (DatabaseException e) {
			throw new PreconditionErrorException(e, changeLog, this);

		} finally {
			JdbcUtils.closeStatement(statement);
		}
	}

    @Override
    public String getName() {
        return "columnExists";
    }
}
