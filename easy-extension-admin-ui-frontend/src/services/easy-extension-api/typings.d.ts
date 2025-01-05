// @ts-ignore
/* eslint-disable */

declare namespace API {
  type MatcherParamInfo = {
    classInfo: ClassInfo;
  };

  type DefaultImplInfo = {
    classInfo: ClassInfo;
  };

  type ExtensionPointInfo = {
    id: string;
    classInfo: ClassInfo;
    defaultImplCode: string;
  };

  type AbilityInfo = {
    code: string;
    implExtensionPoints: Array<string>;
    classInfo?: ClassInfo;
  };

  type BusinessInfo = {
    code: string;
    priority: number;
    usedAbilities?: Array<UsedAbility>;
    implExtensionPoints: Array<string>;
    classInfo?: ClassInfo;
  };

  type UsedAbility = {
    abilityCode: string;
    priority: number;
  };

  type ClassInfo = {
    name?: string;
    fullName?: string;
    sourceCode?: string;
    comment?: string;
  };

  type ConfigInfo = {
    version: string;
    docUrl?: string;
  };
}
