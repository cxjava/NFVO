/*
 * Copyright (c) 2015 Fraunhofer FOKUS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openbaton.nfvo.api;

import org.openbaton.catalogue.security.User;
import org.openbaton.exceptions.PasswordWeakException;
import org.openbaton.nfvo.security.interfaces.UserManagement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1/users")
public class RestUsers {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private UserManagement userManagement;

    /**
     * Adds a new User to the Users repository
     *
     * @param user
     * @return user
     */
    @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public User create(@RequestBody @Valid User user) throws PasswordWeakException {
        log.info("Adding user: " + user.getUsername());
        return userManagement.add(user);
    }

    /**
     * Removes the User from the Users repository
     *
     * @param id : the username of user to be removed
     */

    @RequestMapping(value = "{id}", method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") String id) {
        if (userManagement != null) {
            log.info("removing User with id " + id);
            userManagement.delete(userManagement.queryById(id));
        }
        return;
    }

    /**
     * Returns the list of the Users available
     *
     * @return List<User>: The list of Users available
     */
    @RequestMapping(method = RequestMethod.GET)
    public Iterable<User> findAll() {
        log.trace("Find all Users");
        return userManagement.query();

    }

    /**
     * Returns the User selected by username
     *
     * @param id : The username of the User
     * @return User: The User selected
     */
    @RequestMapping(value = "{username}", method = RequestMethod.GET)
    public User findById(@PathVariable("username") String id) {
        log.trace("find User with username " + id);
        User user = userManagement.query(id);
        log.trace("Found User: " + user);
        return user;
    }

    @RequestMapping(value = "current", method = RequestMethod.GET)
    public User findCurrentUser() {
        User user = userManagement.getCurrentUser();
        log.trace("Found User: " + user);
        return user;
    }

    /**
     * Updates the User
     *
     * @param new_user : The User to be updated
     * @return User The User updated
     */

    @RequestMapping(value = "{username}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public User update(@RequestBody @Valid User new_user) {
        return userManagement.update(new_user);
    }
}
