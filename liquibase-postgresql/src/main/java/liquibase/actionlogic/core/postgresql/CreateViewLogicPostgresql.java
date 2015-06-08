package liquibase.actionlogic.core.postgresql;

import liquibase.Scope;
import liquibase.action.Action;
import liquibase.action.ExecuteSqlAction;
import liquibase.action.core.CreateViewAction;
import liquibase.action.core.StringClauses;
import liquibase.actionlogic.ActionResult;
import liquibase.actionlogic.DelegateResult;
import liquibase.actionlogic.core.CreateViewLogic;
import liquibase.database.Database;
import liquibase.database.core.postgresql.PostgresDatabase;
import liquibase.exception.ActionPerformException;
import liquibase.structure.ObjectName;
import liquibase.structure.core.View;
import liquibase.util.ObjectUtil;

public class CreateViewLogicPostgresql extends CreateViewLogic {
    @Override
    protected Class<? extends Database> getRequiredDatabase() {
        return PostgresDatabase.class;
    }

    @Override
    public ActionResult execute(CreateViewAction action, Scope scope) throws ActionPerformException {
        ActionResult result = super.execute(action, scope);
        if (ObjectUtil.defaultIfEmpty(action.replaceIfExists, false)) {
            Database database = scope.getDatabase();
            ObjectName viewName = action.viewName;

            return new DelegateResult(
                    new ExecuteSqlAction("DROP VIEW IF EXISTS "+database.escapeObjectName(viewName, View.class)),
                    ((DelegateResult) result).getActions().get(0));
        }

        return result;
    }

    @Override
    protected StringClauses generateSql(CreateViewAction action, Scope scope) {
        StringClauses clauses = super.generateSql(action, scope);

        if (ObjectUtil.defaultIfEmpty(action.replaceIfExists, false)) {
            clauses.replace(Clauses.createStatement, "CREATE VIEW");
        }

        return clauses;
    }
}
