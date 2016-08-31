/**
 * Copyright (C) 2014 Xillio (support@xillio.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.xillio.xill.plugins.database.constructs;

import nl.xillio.xill.plugins.database.util.Database;

/**
 * The connect construct for the MYsql database.
 */
@Deprecated
public class MysqlConnectConstruct extends SimplesqlConnectConstruct {

    /**
     * The constructor for the {@link MysqlConnectConstruct}.
     */
    public MysqlConnectConstruct() {
        super(Database.MYSQL.getName(), 3306);
    }

}
