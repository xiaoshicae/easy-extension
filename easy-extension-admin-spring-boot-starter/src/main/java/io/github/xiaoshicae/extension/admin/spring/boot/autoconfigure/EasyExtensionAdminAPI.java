package io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure;

import io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.model.AbilityInfo;
import io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.model.BusinessInfo;
import io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.model.ConfigInfo;
import io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.model.DefaultImplInfo;
import io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.model.ExtensionPointInfo;
import io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.model.MatcherParamInfo;
import io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.model.Response;
import io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.properties.Consts;
import io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.service.ExtensionInfoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST API controller for the easy-extension admin UI.
 */
@RestController
@RequestMapping("${easy-extension.admin.path:/easy-extension-admin}/easy-extension-api")
public class EasyExtensionAdminAPI {
    private final ExtensionInfoService extensionInfoService;

    public EasyExtensionAdminAPI(ExtensionInfoService extensionInfoService) {
        this.extensionInfoService = extensionInfoService;
    }

    @GetMapping("/config-info")
    public Response<ConfigInfo> getConfigInfo() {
        return Response.OK(extensionInfoService.getConfigInfo());
    }

    @GetMapping("/matcher-param")
    public Response<MatcherParamInfo> getMatcherParamClassInfo() {
        return Response.OK(extensionInfoService.getMatcherParamInfo());
    }

    @GetMapping("/default-implementation")
    public Response<DefaultImplInfo> getDefaultImplInfo() {
        return Response.OK(extensionInfoService.getDefaultImplInfo());
    }

    /**
     * Get a paginated list of extension points.
     *
     * @param offset starting index (0-based)
     * @param limit  number of items to return (capped at {@value Consts#DEFAULT_MAX_PAGINATION_LIMIT})
     * @return paginated list of extension points
     */
    @GetMapping("/extension-points")
    public Response<List<ExtensionPointInfo>> getExtensionPoints(
            @RequestParam(value = "offset", defaultValue = "0") int offset,
            @RequestParam(value = "limit", defaultValue = "50") int limit) {
        int cappedLimit = Math.min(Math.max(limit, 1), Consts.DEFAULT_MAX_PAGINATION_LIMIT);
        List<ExtensionPointInfo> all = extensionInfoService.getAllExtensionPoints();
        int fromIndex = Math.min(offset, all.size());
        int toIndex = Math.min(fromIndex + cappedLimit, all.size());
        return Response.OK(all.subList(fromIndex, toIndex), all.size());
    }

    /**
     * Get a paginated list of abilities.
     *
     * @param offset starting index (0-based)
     * @param limit  number of items to return (capped at {@value Consts#DEFAULT_MAX_PAGINATION_LIMIT})
     * @return paginated list of abilities
     */
    @GetMapping("/abilities")
    public Response<List<AbilityInfo>> getAbilities(
            @RequestParam(value = "offset", defaultValue = "0") int offset,
            @RequestParam(value = "limit", defaultValue = "50") int limit) {
        int cappedLimit = Math.min(Math.max(limit, 1), Consts.DEFAULT_MAX_PAGINATION_LIMIT);
        List<AbilityInfo> all = extensionInfoService.getAllAbilities();
        int fromIndex = Math.min(offset, all.size());
        int toIndex = Math.min(fromIndex + cappedLimit, all.size());
        return Response.OK(all.subList(fromIndex, toIndex), all.size());
    }

    /**
     * Get a paginated list of businesses.
     *
     * @param offset starting index (0-based)
     * @param limit  number of items to return (capped at {@value Consts#DEFAULT_MAX_PAGINATION_LIMIT})
     * @return paginated list of businesses
     */
    @GetMapping("/businesses")
    public Response<List<BusinessInfo>> getBusinesses(
            @RequestParam(value = "offset", defaultValue = "0") int offset,
            @RequestParam(value = "limit", defaultValue = "50") int limit) {
        int cappedLimit = Math.min(Math.max(limit, 1), Consts.DEFAULT_MAX_PAGINATION_LIMIT);
        List<BusinessInfo> all = extensionInfoService.getAllBusiness();
        int fromIndex = Math.min(offset, all.size());
        int toIndex = Math.min(fromIndex + cappedLimit, all.size());
        return Response.OK(all.subList(fromIndex, toIndex), all.size());
    }

    /**
     * Manually trigger cache invalidation.
     * <p>
     * This is useful when extension points, abilities, or businesses are registered
     * at runtime after the initial application startup. The cache will be automatically
     * invalidated on the next request.
     * </p>
     *
     * @return success response
     */
    @PostMapping("/cache/refresh")
    public Response<Void> refreshCache() {
        extensionInfoService.invalidateCache();
        return Response.OK(null);
    }
}
