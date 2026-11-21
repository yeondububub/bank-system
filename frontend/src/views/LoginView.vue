<template>
  <div class="auth-container">
    <div class="auth-card">
      <div class="brand-header">
        <h1 class="brand-title">BANK SYSTEM</h1>
        <p class="brand-subtitle">
          {{ isLoginMode ? '이메일로 간편하게 로그인하세요' : '새 계정을 만들고 계좌를 개설해 보세요' }}
        </p>
      </div>

      <div class="tab-switcher" role="tablist">
        <button
          type="button"
          role="tab"
          :aria-selected="isLoginMode"
          :class="['tab-btn', { active: isLoginMode }]"
          @click="isLoginMode = true"
        >
          로그인
        </button>
        <button
          type="button"
          role="tab"
          :aria-selected="!isLoginMode"
          :class="['tab-btn', { active: !isLoginMode }]"
          @click="isLoginMode = false"
        >
          회원가입
        </button>
      </div>

      <form v-if="isLoginMode" @submit.prevent="handleLogin" class="auth-form">
        <div class="form-group">
          <label for="login-email">이메일</label>
          <input
            id="login-email"
            type="email"
            v-model="loginForm.email"
            placeholder="example@bank.com"
            autocomplete="email"
            required
          />
        </div>

        <div class="form-group">
          <label for="login-password">비밀번호</label>
          <div class="password-input-wrapper">
            <input
              id="login-password"
              :type="showPassword ? 'text' : 'password'"
              v-model="loginForm.password"
              placeholder="비밀번호 입력"
              autocomplete="current-password"
              required
            />
            <button
              type="button"
              class="eye-btn"
              :aria-label="showPassword ? '비밀번호 숨기기' : '비밀번호 보기'"
              @click="showPassword = !showPassword"
            >
              <Eye v-if="!showPassword" :size="18" />
              <EyeOff v-else :size="18" />
            </button>
          </div>
        </div>

        <div v-if="errorMessage" class="error-banner" role="alert">
          <AlertCircle :size="16" />
          <span>{{ errorMessage }}</span>
        </div>

        <button type="submit" class="submit-btn" :disabled="isLoading">
          {{ isLoading ? '로그인 중…' : '로그인' }}
        </button>
      </form>

      <form v-else @submit.prevent="handleSignUp" class="auth-form">
        <div class="form-group">
          <label for="signup-name">이름</label>
          <input
            id="signup-name"
            type="text"
            v-model="signUpForm.name"
            placeholder="홍길동"
            autocomplete="name"
            required
          />
        </div>

        <div class="form-group">
          <label for="signup-email">이메일</label>
          <input
            id="signup-email"
            type="email"
            v-model="signUpForm.email"
            placeholder="example@bank.com"
            autocomplete="email"
            required
          />
        </div>

        <div class="form-group">
          <label for="signup-password">비밀번호 (6자 이상)</label>
          <div class="password-input-wrapper">
            <input
              id="signup-password"
              :type="showPassword ? 'text' : 'password'"
              v-model="signUpForm.password"
              placeholder="비밀번호 입력"
              autocomplete="new-password"
              required
              minlength="6"
            />
            <button
              type="button"
              class="eye-btn"
              :aria-label="showPassword ? '비밀번호 숨기기' : '비밀번호 보기'"
              @click="showPassword = !showPassword"
            >
              <Eye v-if="!showPassword" :size="18" />
              <EyeOff v-else :size="18" />
            </button>
          </div>
        </div>

        <div v-if="errorMessage" class="error-banner" role="alert">
          <AlertCircle :size="16" />
          <span>{{ errorMessage }}</span>
        </div>

        <button type="submit" class="submit-btn" :disabled="isLoading">
          {{ isLoading ? '가입 처리 중…' : '회원가입' }}
        </button>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { Eye, EyeOff, AlertCircle } from 'lucide-vue-next'

const router = useRouter()

const isLoginMode = ref(true)
const showPassword = ref(false)
const isLoading = ref(false)
const errorMessage = ref('')

const loginForm = reactive({
  email: '',
  password: ''
})

const signUpForm = reactive({
  name: '',
  email: '',
  password: ''
})

const handleLogin = async () => {
  errorMessage.value = ''
  isLoading.value = true

  try {
    const res = await fetch('/api/v1/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(loginForm)
    })

    if (res.ok) {
      const data = await res.json()
      localStorage.setItem('accessToken', data.accessToken)

      const userInfo = data.user || data
      const userObj = {
        id: userInfo.id || data.userId,
        email: userInfo.email || data.email,
        name: userInfo.name || data.name,
        role: userInfo.role || data.role
      }

      localStorage.setItem('user', JSON.stringify(userObj))

      const roleUpper = String(userObj.role || '').toUpperCase()
      if (roleUpper === 'ADMIN') {
        router.push('/admin')
      } else {
        router.push('/')
      }
    } else {
      const errData = await res.json()
      errorMessage.value = errData.message || '이메일 또는 비밀번호가 일치하지 않습니다.'
    }
  } catch (e) {
    errorMessage.value = '로그인 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.'
  } finally {
    isLoading.value = false
  }
}

const handleSignUp = async () => {
  errorMessage.value = ''
  isLoading.value = true

  try {
    const res = await fetch('/api/v1/auth/signup', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(signUpForm)
    })

    if (res.ok) {
      alert('회원가입이 완료되었습니다. 로그인해 주세요.')
      isLoginMode.value = true
      loginForm.email = signUpForm.email
      loginForm.password = ''
    } else {
      const errData = await res.json()
      errorMessage.value = errData.message || '회원가입 실패. 이미 등록된 이메일일 수 있습니다.'
    }
  } catch (e) {
    errorMessage.value = '회원가입 중 오류가 발생했습니다.'
  } finally {
    isLoading.value = false
  }
}
</script>

<style scoped>
.auth-container {
  min-height: 100dvh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: var(--bg-color);
  padding: 24px;
}

.auth-card {
  width: 100%;
  max-width: 420px;
  background: var(--surface-white);
  border-radius: var(--radius-xl);
  padding: 40px 32px;
  box-shadow: var(--shadow-sm);
}

.brand-header {
  margin-bottom: 28px;
}

.brand-title {
  color: var(--text-primary);
  font-size: 22px;
  font-weight: 800;
  letter-spacing: -0.5px;
  margin-bottom: 6px;
}

.brand-subtitle {
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 500;
}

.tab-switcher {
  display: flex;
  background: var(--bg-color);
  padding: 4px;
  border-radius: var(--radius-md);
  margin-bottom: 24px;
}

.tab-btn {
  flex: 1;
  padding: 10px;
  border: none;
  background: transparent;
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 600;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: background-color 0.15s ease, color 0.15s ease;
}

.tab-btn.active {
  background: var(--surface-white);
  color: var(--text-primary);
  font-weight: 700;
  box-shadow: var(--shadow-xs);
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-group label {
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 600;
}

.form-group input {
  width: 100%;
  padding: 14px 16px;
  background: var(--surface-subtle);
  border: 1px solid transparent;
  border-radius: var(--radius-md);
  color: var(--text-primary);
  font-size: 15px;
  transition: background-color 0.15s ease, border-color 0.15s ease;
}

.form-group input:focus {
  outline: none;
  background: var(--surface-white);
  border-color: var(--toss-blue);
  box-shadow: 0 0 0 3px var(--toss-blue-glow);
}

.password-input-wrapper {
  position: relative;
}

.password-input-wrapper input {
  padding-right: 48px;
}

.eye-btn {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  cursor: pointer;
  color: var(--text-tertiary);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 4px;
}

.eye-btn:hover {
  color: var(--text-primary);
}

.error-banner {
  background: var(--bank-red-light);
  color: var(--bank-red);
  padding: 12px 14px;
  border-radius: var(--radius-md);
  font-size: 13px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
}

.submit-btn {
  margin-top: 8px;
  padding: 15px;
  background: var(--toss-blue);
  color: #FFFFFF;
  border: none;
  border-radius: var(--radius-md);
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  transition: background-color 0.15s ease, transform 0.1s ease;
}

.submit-btn:hover:not(:disabled) {
  background: var(--toss-blue-hover);
}

.submit-btn:active:not(:disabled) {
  transform: scale(0.99);
}

.submit-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
