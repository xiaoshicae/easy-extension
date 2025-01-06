export default {
  'navBar.lang': 'Languages',
  'navBar.doc': 'Document',

  'pages.404.subTitle': 'page not found',
  'pages.404.buttonText': 'Home',

  'menu.extension-point': 'ExtensionPoint',
  'menu.ability': 'Ability',
  'menu.business': 'Business',

  'component.search.button-name': 'Search',
  'component.pagination.show-total': `Total {total} items`,

  // page extension points
  'page.extension-point-list.desc':
    'The extension point is an extension capability provided by the system, essentially an interface, through which business parties can extend by implementing their own customizations.',
  'page.extension-point-list.doc': 'Document',
  'page.extension-point-list.search-placeholder':
    'please input extension point name, description ...',
  'page.extension-point-list.card-modal.api-define': 'Interface Define',
  'page.extension-point-list.card-btn.api-define': 'Interface Define',
  'page.extension-point-list.card-modal.default-impl': 'Default Impl',
  'page.extension-point-list.card-btn.default-impl': 'Default Impl',

  // page abilities
  'page.abilities.desc':
    "Ability is the implementation of a set of extension points and represents the general product capabilities provided by the system. Business can inherit the capability's extension point implementations by mounting the ability.",
  'page.abilities.search-placeholder': 'please input ability name, description ...',
  'page.abilities.card-modal.api-define': 'Ability Define',
  'page.abilities.card-btn.api-define': 'Ability Define',
  'page.abilities.card-modal.ext-impl': 'Extension Impl',
  'page.abilities.card-btn.ext-impl': 'Extension Impl',

  // page businesses
  'page.businesses.desc':
    'The business, as the system integrator, can inherit the implementation of ability extension points by mounting the abilities. If there is no suitable implementation for a ability, the business can also provide custom implementations for the extension points.',
  'page.businesses.search-placeholder': 'please input business name, description ...',
  'page.businesses.card-modal.source-code': 'Biz Define',
  'page.businesses.card-btn.source-code': 'Biz Define',
  'page.businesses.card-modal.ext-impl': 'Ext Impl',
  'page.businesses.card-btn.ext-impl': 'Ext Impl',
  'page.businesses.card-modal.used-abilities': 'Business And Used Abilities Extension Point Conflict Analysis',
  'page.businesses.card-btn.used-abilities': 'Conflict',
  'page.businesses.card-modal.none-abilities': 'none abilities',

  'page.businesses.extension-point-info-table.desc': 'If the business and abilities implement the same extension point, there may be conflicts in the execution of the extension point. The system will judge and select the effective implementation based on priority.',
  'page.businesses.extension-point-info-table.tip': '✅ indicates that the extension point has been implemented',
  'page.businesses.extension-point-info-table.column.ext-impl': 'ExtImpl',
  'page.businesses.extension-point-info-table.column.priority': 'Priority',
  'page.businesses.extension-point-info-table.column.ext-type': 'Type',
  'page.businesses.extension-point-info-table.conflict.note': 'The business and abilities have multiple implementations at this extension point, which may lead to conflicts. In case of conflicts, the implementation with the highest priority will be chosen.',
};
