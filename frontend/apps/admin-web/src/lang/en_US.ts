export default {
  validation: {
    format: { invalid: 'Invalid format' },
    email: { invalid: 'Invalid email format' },
    phone: { mobile: { invalid: 'Invalid mainland mobile number' }, telephone: { invalid: 'Invalid telephone number' }, e164: { invalid: 'Invalid international phone number' } },
    idCard: { mainland: { format: 'Invalid mainland identity card format', date: 'Invalid mainland identity card birth date', checksum: 'Invalid mainland identity card checksum' }, hk: { invalid: 'Invalid Hong Kong identity card format' }, mo: { invalid: 'Invalid Macao identity card format' }, tw: { invalid: 'Invalid Taiwan identity card format' } },
    permit: { hkMacao: { invalid: 'Invalid Hong Kong/Macao residence permit format' }, tw: { invalid: 'Invalid Taiwan residence permit format' } },
    travelPermit: { hkMacao: { invalid: 'Invalid Hong Kong/Macao mainland travel permit format' }, tw: { invalid: 'Invalid Taiwan mainland travel permit format' } },
    creditCode: { format: 'Invalid unified social credit code format', character: 'Unified social credit code contains an invalid character', checksum: 'Invalid unified social credit code checksum' }
  },
  // 路由国际化
  route: {
    dashboard: 'Dashboard',
    document: 'Document'
  },
  // 登录页面国际化
  login: {
    username: 'Username',
    password: 'Password',
    login: 'Login',
    logging: 'Logging...',
    code: 'Verification Code',
    rememberPassword: 'Remember me',
    switchRegisterPage: 'Sign up now',
    rule: {
      username: {
        required: 'Please enter your account'
      },
      password: {
        required: 'Please enter your password'
      },
      code: {
        required: 'Please enter a verification code'
      }
    },
    social: {
      wechat: 'Wechat Login',
      maxkey: 'MaxKey Login',
      topiam: 'TopIam Login',
      gitee: 'Gitee Login',
      github: 'Github Login'
    }
  },
  // 注册页面国际化
  register: {
    username: 'Username',
    password: 'Password',
    confirmPassword: 'Confirm Password',
    register: 'Register',
    registering: 'Registering...',
    registerSuccess: 'Congratulations, your {username} account has been registered!',
    code: 'Verification Code',
    switchLoginPage: 'Log in with an existing account',
    rule: {
      username: {
        required: 'Please enter your account',
        length: 'The length of the user account must be between {min} and {max}'
      },
      password: {
        required: 'Please enter your password',
        length: 'The user password must be between {min} and {max} in length',
        pattern: "Can't contain illegal characters: {strings}"
      },
      code: {
        required: 'Please enter a verification code'
      },
      confirmPassword: {
        required: 'Please enter your password again',
        equalToPassword: 'The password entered twice is inconsistent'
      }
    }
  },
  passwordPolicy: {
    unavailable: 'Password policy is unavailable',
    PASSWORD_TOO_SHORT: 'Password must contain at least {min} characters',
    PASSWORD_TOO_LONG: 'Password must contain no more than {max} characters',
    PASSWORD_MISSING_UPPERCASE: 'Password must contain an uppercase letter',
    PASSWORD_MISSING_LOWERCASE: 'Password must contain a lowercase letter',
    PASSWORD_MISSING_DIGIT: 'Password must contain a digit',
    PASSWORD_MISSING_SPECIAL: 'Password must contain one of these special characters: {specials}',
    PASSWORD_CONTAINS_DISALLOWED_CHARACTER:
      'Password may contain only letters, digits, and these special characters: {specials}'
  },
  // 导航栏国际化
  navbar: {
    full: 'Full Screen',
    language: 'Language',
    dashboard: 'Dashboard',
    document: 'Document',
    message: 'Message',
    layoutSize: 'Layout Size',
    layoutSetting: 'Layout Setting',
    personalCenter: 'Personal Center',
    logout: 'Logout'
  }
};
