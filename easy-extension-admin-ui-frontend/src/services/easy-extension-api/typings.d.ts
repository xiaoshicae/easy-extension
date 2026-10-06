// @ts-ignore
/* eslint-disable */

declare namespace API {
  type MatcherParamInfo = {
    // null when the matcher parameter type cannot be derived (no Matcher<T> generics, no configured type)
    classInfo: ClassInfo | null;
  };

  type DefaultImplInfo = {
    // one class may back several extension points; extension points with a framework-provided no-op default are not listed
    classInfos: Array<ClassInfo>;
  };

  type ExtensionPointInfo = {
    id: string;
    classInfo: ClassInfo;
    defaultImplCode: string;
    scenarios: Array<string>;
    version: number;
  };

  type AbilityInfo = {
    code: string;
    implExtensionPoints: Array<string>;
    classInfo?: ClassInfo;
  };

  type BusinessInfo = {
    code: string;
    // position in the business' `abilities` order (0 = highest); the business itself sits where Self is declared
    priority: number;
    usedAbilities?: Array<UsedAbility>;
    implExtensionPoints: Array<string>;
    classInfo?: ClassInfo;
  };

  type UsedAbility = {
    abilityCode: string;
    // position in the business' `abilities` order (0 = highest)
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
