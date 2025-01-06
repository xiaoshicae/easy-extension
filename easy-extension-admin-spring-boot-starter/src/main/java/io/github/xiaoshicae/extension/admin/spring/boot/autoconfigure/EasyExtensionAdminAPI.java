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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(Consts.API_URL_PREFIX)
public class EasyExtensionAdminAPI {
    ExtensionInfoService extensionInfoService;

    public EasyExtensionAdminAPI(ExtensionInfoService extensionInfoService) {
        this.extensionInfoService = extensionInfoService;
    }

    @GetMapping("/config-info")
    public Response<ConfigInfo> getConfigInfo() {
        return Response.OK(extensionInfoService.getConfigInfo());
    }

    @GetMapping("/matcher-param")
    public Response<MatcherParamInfo> getMatcherParamClassInfo() {
        return  Response.OK(extensionInfoService.getMatcherParamInfo());
    }

    @GetMapping("/default-implementation")
    public Response<DefaultImplInfo> getDefaultImplInfo() {
        return Response.OK(extensionInfoService.getDefaultImplInfo());
    }

    @GetMapping("/all/extension-points")
    public Response<List<ExtensionPointInfo>> getAllExtensionPoints() {
        return Response.OK(extensionInfoService.getAllExtensionPoints());
    }

    @GetMapping("/all/abilities")
    public Response<List<AbilityInfo>> getAllAbilities() {
        return Response.OK(extensionInfoService.getAllAbilities());
    }

    @GetMapping("/all/businesses")
    public Response<List<BusinessInfo>> getAllBusiness() {
        return Response.OK(extensionInfoService.getAllBusiness());
    }
}
