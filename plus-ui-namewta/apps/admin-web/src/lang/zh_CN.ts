export default {
  validation: {
    format: { invalid: '格式不正确' },
    email: { invalid: '邮箱格式错误' },
    phone: { mobile: { invalid: '手机号格式错误' }, telephone: { invalid: '电话号码格式错误' }, e164: { invalid: '国际电话号码格式错误' } },
    idCard: { mainland: { format: '大陆身份证格式错误', date: '大陆身份证出生日期无效', checksum: '大陆身份证校验码错误' }, hk: { invalid: '香港居民身份证格式错误' }, mo: { invalid: '澳门居民身份证格式错误' }, tw: { invalid: '台湾居民身份证格式错误' } },
    permit: { hkMacao: { invalid: '港澳居民居住证格式错误' }, tw: { invalid: '台湾居民居住证格式错误' } },
    travelPermit: { hkMacao: { invalid: '港澳居民来往内地通行证格式错误' }, tw: { invalid: '台湾居民来往大陆通行证格式错误' } },
    creditCode: { format: '统一社会信用代码格式错误', character: '统一社会信用代码包含非法字符', checksum: '统一社会信用代码校验码错误' }
  },
  // 路由国际化
  route: {
    dashboard: '首页',
    document: '项目文档'
  },
  // 登录页面国际化
  login: {
    username: '用户名',
    password: '密码',
    login: '登 录',
    logging: '登 录 中...',
    code: '验证码',
    rememberPassword: '记住我',
    switchRegisterPage: '立即注册',
    rule: {
      username: {
        required: '请输入您的账号'
      },
      password: {
        required: '请输入您的密码'
      },
      code: {
        required: '请输入验证码'
      }
    },
    social: {
      wechat: '微信登录',
      maxkey: 'MaxKey登录',
      topiam: 'TopIam登录',
      gitee: 'Gitee登录',
      github: 'Github登录'
    }
  },
  // 注册页面国际化
  register: {
    username: '用户名',
    password: '密码',
    confirmPassword: '确认密码',
    register: '注 册',
    registering: '注 册 中...',
    registerSuccess: '恭喜你，您的账号 {username} 注册成功！',
    code: '验证码',
    switchLoginPage: '使用已有账户登录',
    rule: {
      username: {
        required: '请输入您的账号',
        length: '用户账号长度必须介于 {min} 和 {max} 之间'
      },
      password: {
        required: '请输入您的密码',
        length: '用户密码长度必须介于 {min} 和 {max} 之间',
        pattern: '不能包含非法字符：{strings}'
      },
      code: {
        required: '请输入验证码'
      },
      confirmPassword: {
        required: '请再次输入您的密码',
        equalToPassword: '两次输入的密码不一致'
      }
    }
  },
  passwordPolicy: {
    unavailable: '密码策略配置不可用',
    PASSWORD_TOO_SHORT: '密码长度不能少于 {min} 位',
    PASSWORD_TOO_LONG: '密码长度不能超过 {max} 位',
    PASSWORD_MISSING_UPPERCASE: '密码必须包含大写英文字母',
    PASSWORD_MISSING_LOWERCASE: '密码必须包含小写英文字母',
    PASSWORD_MISSING_DIGIT: '密码必须包含数字',
    PASSWORD_MISSING_SPECIAL: '密码必须包含特殊字符：{specials}',
    PASSWORD_CONTAINS_DISALLOWED_CHARACTER: '密码只能包含英文字母、数字和以下特殊字符：{specials}'
  },
  // 导航栏国际化
  navbar: {
    full: '全屏',
    language: '语言',
    dashboard: '首页',
    document: '项目文档',
    message: '消息',
    layoutSize: '布局大小',
    layoutSetting: '布局设置',
    personalCenter: '个人中心',
    logout: '退出登录'
  }
};
