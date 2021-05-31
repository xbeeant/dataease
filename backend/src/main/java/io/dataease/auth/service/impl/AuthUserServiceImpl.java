package io.dataease.auth.service.impl;

import io.dataease.auth.api.dto.CurrentRoleDto;
import io.dataease.auth.entity.SysUserEntity;
import io.dataease.base.mapper.ext.AuthMapper;
import io.dataease.auth.service.AuthUserService;
import io.dataease.commons.constants.AuthConstants;
import io.dataease.plugins.common.dto.PluginSysMenu;
import io.dataease.plugins.common.service.PluginMenuService;
import io.dataease.plugins.config.SpringContextUtil;
import io.dataease.plugins.util.PluginUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AuthUserServiceImpl implements AuthUserService {


    @Resource
    private AuthMapper authMapper;

    /**
     * 此处需被F2CRealm登录认证调用 也就是说每次请求都会被调用 所以最好加上缓存
     * @param userId
     * @return
     */
    @Cacheable(value = AuthConstants.USER_CACHE_NAME,  key = "'user' + #userId" )
    @Override
    public SysUserEntity getUserById(Long userId){
        return authMapper.findUser(userId);
    }

    @Override
    public SysUserEntity getUserByName(String username) {
        return authMapper.findUserByName(username);
    }

    @Override
    public List<String> roles(Long userId){
        return authMapper.roleCodes(userId);
    }

    /**
     * 此处需被F2CRealm登录认证调用 也就是说每次请求都会被调用 所以最好加上缓存
     * @param userId
     * @return
     */
    @Cacheable(value = AuthConstants.USER_PERMISSION_CACHE_NAME,  key = "'user' + #userId" )
    @Override
    public List<String> permissions(Long userId){
        List<String> permissions = authMapper.permissions(userId);
        List<PluginSysMenu> pluginSysMenus = PluginUtils.pluginMenus();
        if (CollectionUtils.isNotEmpty(pluginSysMenus)) {
            List<Long> menuIds = authMapper.userMenuIds(userId);
            List<String> pluginPermissions = pluginSysMenus.stream().
                    filter(sysMenu -> menuIds.contains(sysMenu.getMenuId()))
                    .map(menu -> menu.getPermission()).collect(Collectors.toList());
            permissions.addAll(pluginPermissions);
        }
        return permissions.stream().filter(StringUtils::isNotEmpty).collect(Collectors.toList());
    }

    /**
     * 此处需被F2CRealm登录认证调用 也就是说每次请求都会被调用 所以最好加上缓存
     * @param userId
     * @return
     */
    @Cacheable(value = AuthConstants.USER_ROLE_CACHE_NAME,  key = "'user' + #userId" )
    @Override
    public List<CurrentRoleDto> roleInfos(Long userId) {
        return authMapper.roles(userId);
    }


    /**
     * 一波清除3个缓存
     * @param userId
     */
    @Caching(evict = {
            @CacheEvict(value = AuthConstants.USER_CACHE_NAME, key = "'user' + #userId"),
            @CacheEvict(value = AuthConstants.USER_ROLE_CACHE_NAME, key = "'user' + #userId"),
            @CacheEvict(value = AuthConstants.USER_PERMISSION_CACHE_NAME, key = "'user' + #userId")
    })
    @Override
    public void clearCache(Long userId) {

    }
}
