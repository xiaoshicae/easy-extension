export default {
  'navBar.lang': '语言',
  'navBar.doc': '文档',

  'pages.404.subTitle': '您访问的页面不存在',
  'pages.404.buttonText': '返回首页',

  'menu.extension-point': '扩展点',
  'menu.ability': '能力',
  'menu.business': '业务',

  'component.search.button-name': '搜索',
  'component.pagination.show-total': `共{total}个`,

  // page extension points
  'page.extension-point-list.desc':
    '扩展点是系统提供的扩展能力，本质就是一个接口，业务方通过自定义实现进行扩展。',
  'page.extension-point-list.doc': '接入文档',
  'page.extension-point-list.search-placeholder': '请输入扩展点名称，描述等信息',
  'page.extension-point-list.card-modal.api-define': '扩展点定义',
  'page.extension-point-list.card-btn.api-define': '扩展点定义',
  'page.extension-point-list.card-modal.default-impl': '默认实现',
  'page.extension-point-list.card-btn.default-impl': '默认实现',

  // page abilities
  'page.abilities.desc':
    '能力是一组扩展点的实现，是系统提供的通用产品能力，业务可以通过挂载能力来继承能力的扩展点实现。',
  'page.abilities.search-placeholder': '请输入能力名称，描述等信息',
  'page.abilities.card-modal.api-define': '能力定义',
  'page.abilities.card-btn.api-define': '能力定义',
  'page.abilities.card-modal.ext-impl': '扩展点实现',
  'page.abilities.card-btn.ext-impl': '扩展点实现',

  // page businesses
  'page.businesses.desc':
    '业务即系统接入方，可以通过挂载能力来继承能力的扩展点实现，如果能力没有合适的实现，业务也可以对扩展点进行自定义实现。',
  'page.businesses.search-placeholder': '请输入业务名称，描述等信息',
  'page.businesses.card-modal.source-code': '业务定义',
  'page.businesses.card-btn.source-code': '业务定义',
  'page.businesses.card-modal.ext-impl': '扩展点实现',
  'page.businesses.card-btn.ext-impl': '扩展点实现',
  'page.businesses.card-modal.used-abilities': '业务和能力实现的扩展点冲突分析',
  'page.businesses.card-btn.used-abilities': '能力冲突分析',
  'page.businesses.card-modal.none-abilities': '未使用任何能力',

  'page.businesses.extension-point-info-table.desc': '业务和能力如果实现了相同的扩展点，那扩展点执行就有可能出现冲突，系统会根据优先级依次判断并选择生效的实现。',
  'page.businesses.extension-point-info-table.tip': '✅ 表示实现了该扩展点',
  'page.businesses.extension-point-info-table.column.ext-impl': '扩展点实现',
  'page.businesses.extension-point-info-table.column.priority': '优先级',
  'page.businesses.extension-point-info-table.column.ext-type': '类型',
  'page.businesses.extension-point-info-table.conflict.note': '业务及能力在该扩展点有多个实现，可能存在冲突。发生冲突时，会选择优先级最高的实现。',
};
