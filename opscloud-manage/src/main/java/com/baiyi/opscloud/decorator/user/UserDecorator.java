package com.baiyi.opscloud.decorator.user;

import com.baiyi.opscloud.common.base.BusinessType;
import com.baiyi.opscloud.common.base.CredentialType;
import com.baiyi.opscloud.common.util.BeanCopierUtils;
import com.baiyi.opscloud.domain.generator.opscloud.*;
import com.baiyi.opscloud.domain.vo.server.ServerGroupVO;
import com.baiyi.opscloud.domain.vo.user.UserApiTokenVO;
import com.baiyi.opscloud.domain.vo.user.UserCredentialVO;
import com.baiyi.opscloud.domain.vo.user.UserGroupVO;
import com.baiyi.opscloud.domain.vo.user.UserVO;
import com.baiyi.opscloud.service.server.OcServerGroupService;
import com.baiyi.opscloud.service.user.OcUserApiTokenService;
import com.baiyi.opscloud.service.user.OcUserCredentialService;
import com.baiyi.opscloud.service.user.OcUserGroupService;
import com.baiyi.opscloud.service.user.OcUserPermissionService;
import com.google.common.collect.Maps;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author baiyi
 * @Date 2020/2/25 12:06 下午
 * @Version 1.0
 */
@Component("UserDecorator")
public class UserDecorator {

    @Resource
    private OcUserGroupService ocUserGroupService;

    @Resource
    private OcServerGroupService ocServerGroupService;

    @Resource
    private OcUserApiTokenService ocUserApiTokenService;

    @Resource
    private OcUserCredentialService ocUserCredentialService;

    @Resource
    private OcUserPermissionService ocUserPermissionService;

    // from mysql
    public UserVO.User decorator(UserVO.User user, Integer extend) {
        user.setPassword("");
        if (extend != null && extend == 1) {
            // 装饰 用户组
            List<OcUserGroup> userGroupList = ocUserGroupService.queryOcUserGroupByUserId(user.getId());
            user.setUserGroups(BeanCopierUtils.copyListProperties(userGroupList, UserGroupVO.UserGroup.class));
            // 装饰 服务器组
            List<OcServerGroup> serverGroupList = ocServerGroupService.queryUserPermissionOcServerGroupByUserId(user.getId());
            user.setServerGroups(convert(user, serverGroupList));
            // 装饰 ApiToken
            List<OcUserApiToken> userApiTokens = ocUserApiTokenService.queryOcUserApiTokenByUsername(user.getUsername());
            List<UserApiTokenVO.UserApiToken> apiTokens = BeanCopierUtils.copyListProperties(userApiTokens, UserApiTokenVO.UserApiToken.class).stream().map(e -> {
                e.setToken("申请后不可查看");
                return e;
            }).collect(Collectors.toList());
            user.setApiTokens(apiTokens);
            // 装饰 凭据
            List<OcUserCredential> credentials = ocUserCredentialService.queryOcUserCredentialByUserId(user.getId());
            Map<String, UserCredentialVO.UserCredential> credentialMap = Maps.newHashMap();
            for (OcUserCredential credential : credentials)
                credentialMap.put(CredentialType.getName(credential.getCredentialType()), BeanCopierUtils.copyProperties(credential, UserCredentialVO.UserCredential.class));
            user.setCredentialMap(credentialMap);
        }
        return user;
    }


    private List<ServerGroupVO.ServerGroup> convert(UserVO.User user, List<OcServerGroup> serverGroupList) {
        return serverGroupList.stream().map(e -> {
            ServerGroupVO.ServerGroup serverGroup = BeanCopierUtils.copyProperties(e, ServerGroupVO.ServerGroup.class);
            OcUserPermission permission = new OcUserPermission();
            permission.setBusinessType(BusinessType.SERVER_ADMINISTRATOR_ACCOUNT.getType());
            permission.setBusinessId(e.getId());
            permission.setUserId(user.getId());
            permission = ocUserPermissionService.queryOcUserPermissionByUniqueKey(permission);
            serverGroup.setIsAdmin(permission != null);
            return serverGroup;
        }).collect(Collectors.toList());
    }
}
