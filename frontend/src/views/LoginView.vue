<template>
  <div class="auth-container">
    <div class="auth-card">
      <div class="brand-header">
        <div class="brand-icon-box">
          <Landmark :size="36" class="brand-icon-svg" />
        </div>
        <h1 class="brand-title"><span class="blue-text">BANK</span> SYSTEM</h1>
        <p class="brand-subtitle">Toss 스타일의 스마트 뱅킹 플랫폼</p>
      </div>

      <!-- Tab Switcher -->
      <div class="tab-switcher">
        <button 
          :class="['tab-btn', { active: isLoginMode }]" 
          @click="isLoginMode = true"
        >
          로그인
        </button>
        <button 
          :class="['tab-btn', { active: !isLoginMode }]" 
          @click="isLoginMode = false"
        >
          회원가입
        </button>
      </div>

      <!-- Login Form -->
      <form v-if="isLoginMode" @submit.prevent="handleLogin" class="auth-form">
        <div class="form-group">
          <label>이메일 주소</label>
          <input 
            type="email" 
            v-model="loginForm.email" 
            placeholder="example@bank.com" 
            required 
          />
        </div>

        <div class="form-group">
          <label>비밀번호</label>
          <div class="password-input-wrapper">
            <input 
              :type="showPassword ? 'text' : 'password'" 
              v-model="loginForm.password" 
              placeholder="비밀번호를 입력하세요" 
              required 
            />
            <button type="button" class="eye-btn" @click="showPassword = !showPassword">
              <Eye v-if="!showPassword" :size="16" />
              <EyeOff v-else :size="16" />
            </button>
          </div>
        </div>

        <div v-if="errorMessage" class="error-banner">
          <AlertCircle :size="16" />
          <span>{{ errorMessage }}</span>
        </div>

        <button type="submit" class="submit-btn" :disabled="isLoading">
          <span v-if="isLoading">로그인 중...</span>
          <span v-else>로그인</span>
        </button>
      </form>

      <!-- Register Form -->
      <form v-else @submit.prevent="handleSignUp" class="auth-form">
        <div class="form-group">
          <label>이름</label>
          <input 
            type="text" 
            v-model="signUpForm.name" 
            placeholder="홍길동" 
            required 
          />
        </div>

        <div class="form-group">
          <label>이메일 주소</label>
          <input 
            type="email" 
            v-model="signUpForm.email" 
            placeholder="example@bank.com" 
            required 
          />
        </div>

        <div class="form-group">
          <label>비밀번호 (최소 6자 이상)</label>
          <div class="password-input-wrapper">
            <input 
              :type="showPassword ? 'text' : 'password'" 
              v-model="signUpForm.password" 
              placeholder="비밀번호를 입력하세요" 
              required 
              minlength="6"
            />
            <button type="button" class="eye-btn" @click="showPassword = !showPassword">
              <Eye v-if="!showPassword" :size="16" />
              <EyeOff v-else :size="16" />
            </button>
          </div>
        </div>

        <div v-if="errorMessage" class="error-banner">
          <AlertCircle :size="16" />
          <span>{{ errorMessage }}</span>
        </div>

        <button type="submit" class="submit-btn" :disabled="isLoading">
          <span v-if="isLoading">회원가입 중...</span>
          <span v-else>회원가입 완료</span>
        </button>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { Landmark, Eye, EyeOff, AlertCircle } from 'lucide-vue-next'

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
  password: '',
  role: 'USER'
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
      alert('회원가입이 완료되었습니다! 로그인해 주세요.')
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
  min-height: 100vh;
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
  padding: 40px;
  box-shadow: var(--shadow-lg);
  border: 1px solid var(--border-light);
}

.brand-header {
  text-align: center;
  margin-bottom: 28px;
}

.brand-icon-box {
  margin-bottom: 12px;
  display: flex;
  justify-content: center;
}

.brand-icon-svg {
  color: var(--toss-blue);
}

.brand-title {
  color: var(--text-primary);
  font-size: 24px;
  font-weight: 800;
  letter-spacing: -0.5px;
  margin: 0 0 4px 0;
}

.blue-text {
  color: var(--toss-blue);
}

.brand-subtitle {
  color: var(--text-secondary);
  font-size: 14px;
  margin: 0;
}

.tab-switcher {
  display: flex;
  background: var(--surface-subtle);
  padding: 4px;
  border-radius: var(--radius-md);
  margin-bottom: 24px;
  border: 1px solid var(--border-light);
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
  transition: all 0.2s ease;
}

.tab-btn.active {
  background: var(--toss-blue);
  color: #ffffff;
  font-weight: 700;
  box-shadow: 0 4px 12px var(--toss-blue-glow);
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
  font-weight: 700;
}

.form-group input {
  width: 100%;
  padding: 14px 16px;
  background: var(--surface-white);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  color: var(--text-primary);
  font-size: 15px;
  outline: none;
  transition: all 0.2s ease;
}

.form-group input:focus {
  border-color: var(--toss-blue);
  box-shadow: 0 0 0 3px var(--toss-blue-light);
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
  border: 1px solid rgba(240, 68, 56, 0.3);
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
  padding: 16px;
  background: var(--toss-blue);
  color: #ffffff;
  border: none;
  border-radius: var(--radius-md);
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 4px 14px var(--toss-blue-glow);
}

.submit-btn:hover:not(:disabled) {
  background: var(--toss-blue-hover);
  transform: translateY(-1px);
}

.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
