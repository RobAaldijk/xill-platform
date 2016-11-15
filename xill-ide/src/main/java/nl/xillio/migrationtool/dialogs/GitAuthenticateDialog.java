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
package nl.xillio.migrationtool.dialogs;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import nl.xillio.xill.versioncontrol.JGitRepository;

public class GitAuthenticateDialog extends FXMLDialog {
    @FXML
    private TextField username;
    @FXML
    private PasswordField password;
    @FXML
    private Label message;

    private final JGitRepository repo;

    /**
     * Default constructor.
     *
     * @param repo    the JGitRepository that will be pushed to.
     */
    public GitAuthenticateDialog(final JGitRepository repo) {
        super("/fxml/dialogs/GitAuthenticate.fxml");
        this.setTitle("Fill in credentials");
        this.repo = repo;
    }

    @FXML
    private void cancelBtnPressed(final ActionEvent event) {
        close();
    }

    @FXML
    private void okBtnPressed(final ActionEvent event) {
        repo.setCredentials(username.getText(), password.getText());
        password.clear();
        close();
    }
}

