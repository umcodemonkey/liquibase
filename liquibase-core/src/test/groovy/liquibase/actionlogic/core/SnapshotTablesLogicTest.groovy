package liquibase.actionlogic.core

import liquibase.JUnitScope
import liquibase.Scope
import liquibase.action.core.SnapshotDatabaseObjectsAction
import liquibase.actionlogic.RowBasedQueryResult
import liquibase.sdk.database.MockDatabase
import liquibase.structure.core.Column
import liquibase.structure.core.Table
import spock.lang.Specification
import spock.lang.Unroll

class SnapshotTablesLogicTest extends Specification {

    @Unroll
    def "convertObject handles column name and table plus container correctly"() {
        when:
        def database = new MockDatabase();
        database.setMaxContainerDepth(maxDepth)
        def scope = JUnitScope.getInstance(database)

        def object = new SnapshotTablesLogic().convertToObject(new RowBasedQueryResult.Row([
                TABLE_CAT  : tableCat,
                TABLE_SCHEM: tableSchema,
                TABLE_NAME : tableName,
        ]), new SnapshotDatabaseObjectsAction(Column, new Table()), scope)

        then:
        object instanceof Table
        object.name.toString() == expected

        where:
        tableCat   | tableSchema  | tableName   | maxDepth | expected
        "tableCat" | "schemaName" | "tableName" | 2        | "tableCat.schemaName.tableName"
        null       | "schemaName" | "tableName" | 2        | "#DEFAULT.schemaName.tableName"
        "tableCat" | null         | "tableName" | 2        | "tableCat.#DEFAULT.tableName"
        "tableCat" | "schemaName" | "tableName" | 1        | "schemaName.tableName"
        "tableCat" | null         | "tableName" | 1        | "tableCat.tableName"
        null       | "schemaName" | "tableName" | 1        | "schemaName.tableName"
        null       | null         | "tableName" | 0        | "tableName"
    }

}
