/*
 * Copyright (c) 2020-present The Aspectran Project
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
package com.aspectran.aspectow.console.user;

import com.aspectran.aspectow.console.auth.UserInfo;
import com.aspectran.aspectow.console.common.db.model.LoginHistory;
import com.aspectran.aspectow.console.common.db.model.Permission;
import com.aspectran.aspectow.console.common.db.model.Role;
import com.aspectran.aspectow.console.common.db.model.User;
import com.aspectran.aspectow.console.common.pagination.PageInfo;
import com.aspectran.aspectow.console.common.service.UserService;
import com.aspectran.core.activity.Translet;
import com.aspectran.core.component.bean.annotation.Action;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Dispatch;
import com.aspectran.core.component.bean.annotation.Request;
import com.aspectran.core.component.bean.annotation.RequestToPost;
import com.aspectran.utils.StringUtils;
import com.aspectran.web.activity.response.RestResponse;
import com.aspectran.web.support.rest.response.FailureResponse;
import com.aspectran.web.support.rest.response.SuccessResponse;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Map;

/**
 * Controller class that handles user management requests, including
 * listing users, managing login history, and creating, updating, or deleting users.
 */
@Component("/user")
public class UserManagementActivity {

    private final UserService userService;

    /**
     * Constructs a new {@code UserManagementActivity} with the specified user service.
     * @param userService the user service
     */
    @Autowired
    public UserManagementActivity(UserService userService) {
        this.userService = userService;
    }

    /**
     * Displays the user list page containing all users and available roles.
     * @return a map of attributes for rendering the view
     */
    @Request("/")
    @Dispatch("user/list")
    @Action("page")
    public Map<String, Object> list(@NonNull Translet translet) {
        PageInfo pageInfo = PageInfo.of(translet, "user_list_page_size");
        List<User> userList = userService.getUserList(pageInfo);
        List<Role> roleList = userService.getRoleList();
        List<Permission> permissionList = userService.getPermissionList();
        return Map.of(
            "title", "Users",
            "style", "user-management-page",
            "group", "accounts-menu",
            "userList", userList,
            "roleList", roleList,
            "permissionList", permissionList,
            "pageInfo", pageInfo
        );
    }

    /**
     * Displays the login history page for a given user or the current user if not an admin.
     * @param translet the current translet
     * @param username the target username
     * @return a map of attributes for rendering the view
     */
    @Request("/login-history")
    @Dispatch("user/login-history")
    @Action("page")
    public Map<String, Object> loginHistory(@NonNull Translet translet, String username) {
        UserInfo userInfo = translet.getSessionAdapter().getAttribute(UserInfo.USERINFO_KEY);
        String targetUsername = username;

        // If not an admin, force to see only their own history
        if (userInfo != null && !userInfo.hasRole("SUPER_ADMIN")) {
            targetUsername = userInfo.getUsername();
        }

        PageInfo pageInfo = PageInfo.of(translet, "login_history_page_size");
        List<LoginHistory> historyList = userService.getLoginHistoryList(targetUsername, pageInfo);
        return Map.of(
            "title", "Login History",
            "style", "login-history-page",
            "group", "accounts-menu",
            "historyList", historyList,
            "pageInfo", pageInfo,
            "username", (targetUsername != null ? targetUsername : "")
        );
    }

    /**
     * Saves user details, either creating a new user or updating an existing one,
     * along with their assigned roles.
     * @param user the user data
     * @param roleIds the array of role IDs to associate with the user
     * @return a {@link RestResponse} representing success or failure of the operation
     */
    @RequestToPost("/save")
    public RestResponse save(@NonNull User user, Long[] roleIds) {
        if (StringUtils.isEmpty(user.getUsername())) {
            return new FailureResponse().setError("required", "Username is required.");
        }

        List<Long> roleIdList = (roleIds != null ? List.of(roleIds) : null);

        if (user.getUserId() != null) {
            // Update
            User existing = userService.getUserById(user.getUserId());
            if (existing == null) {
                 return new FailureResponse().setError("not_found", "User not found.");
            }
            userService.updateUser(user, roleIdList);
            return new SuccessResponse("Updated").ok();
        } else {
            // Insert
            if (userService.getUserByUsername(user.getUsername()) != null) {
                return new FailureResponse().setError("duplicate", "Username already exists.");
            }
            if (StringUtils.isEmpty(user.getPassword())) {
                return new FailureResponse().setError("required", "Password is required for a new user.");
            }
            userService.createUser(user, roleIdList);
            return new SuccessResponse("Created").ok();
        }
    }

    /**
     * Deletes the user with the specified user ID.
     * @param userId the ID of the user to delete
     * @return a {@link RestResponse} representing success or failure of the operation
     */
    @RequestToPost("/delete")
    public RestResponse delete(Long userId) {
        if (userId == null) {
            return new FailureResponse().setError("required", "User ID is required.");
        }
        userService.deleteUser(userId);
        return new SuccessResponse("Deleted").ok();
    }

    /**
     * Saves permission mapping for a specific role.
     * @param roleId the role ID
     * @param permIds the array of permission IDs to associate with the role
     * @return a {@link RestResponse} representing success or failure of the operation
     */
    @RequestToPost("/role/save-permissions")
    public RestResponse saveRolePermissions(Long roleId, Long[] permIds) {
        if (roleId == null) {
            return new FailureResponse().setError("required", "Role ID is required.");
        }
        List<Long> permIdList = (permIds != null ? List.of(permIds) : null);
        userService.updateRolePermissions(roleId, permIdList);
        return new SuccessResponse("Role permissions updated").ok();
    }

}
