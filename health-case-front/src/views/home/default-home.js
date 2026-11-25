const supportUrl = 'https://work.weixin.qq.com/kfid/kfcbca36d8d108da2db'

export const defaultBannerImage = `${process.env.BASE_URL}images/caringhealth.png`

// An installation without a published homepage can still show its usual service links.
export const defaultHome = {
  document: {
    title: '健康服务',
    primaryColor: '#1677ff',
    showNavigation: true,
    homeLabel: '首页',
    mineLabel: '个人信息',
    blocks: [
      { id: 'home-heading', type: 'heading', title: '健康服务', description: '为您提供便捷、专业的健康服务' },
      { id: 'home-banner', type: 'banner' },
      {
        id: 'home-functions', type: 'links', title: '常用服务', layout: 'grid', columns: 4, showMore: true,
        items: [
          { id: 'submit', title: '入组提交', icon: 'Money', tone: 'red', linkUrl: '/submit' },
          { id: 'policy', title: '我的保单', icon: 'Tickets', tone: 'green', linkUrl: '/policy' },
          { id: 'claim', title: '理赔查询', icon: 'Search', tone: 'orange', linkUrl: '/service-record' },
          { id: 'records', title: '服务记录', icon: 'Document', tone: 'purple', linkUrl: '/service-record' },
          { id: 'support', title: '在线客服', icon: 'Service', tone: 'blue', linkUrl: supportUrl },
          { id: 'health', title: '健康服务', icon: 'FirstAidKit', tone: 'pink', linkUrl: '/submit' }
        ]
      },
      {
        id: 'home-services', type: 'links', title: '我的服务', layout: 'list',
        items: [
          { id: 'my-claim', title: '我的理赔', description: '查看历史服务记录与处理进度', icon: 'Money', tone: 'blue', linkUrl: '/service-record' },
          { id: 'my-submit', title: '资料提交', description: '上传病例相关资料', icon: 'Document', tone: 'blue', linkUrl: '/submit' }
        ]
      }
    ]
  },
  images: {},
  banners: [
    { id: 'home-health', assetKey: 'home-health', title: '健康服务', description: '便捷、专业的健康服务', linkUrl: '/submit', imageUrl: defaultBannerImage },
    { id: 'home-claim', title: '理赔服务', description: '快速提交资料，查询服务进度', linkUrl: '/service-record', tone: 'blue' },
    { id: 'home-support', title: '联系客服', description: '在线咨询，获取专业服务帮助', linkUrl: supportUrl, tone: 'green' }
  ]
}
