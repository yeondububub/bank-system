<template>
  <div class="auth-container">
    <div class="auth-card">
      <div class="brand-header">
        <div class="brand-icon">🏛️</div>
        <h1 class="brand-title">BANK SYSTEM</h1>
        <p class="brand-subtitle">안전하고 스마트한 인터넷 뱅킹 서비스</p>
      </div>

      <!-- 탭 스위처 -->
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

      <!-- 로그인 폼 -->
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
              {{ showPassword ? '👁️' : '🔒' }}
            </button>
          </div>
        </div>

        <div v-if="errorMessage" class="error-banner">
          ⚠️ {{ errorMessage }}
        </div>

        <button type="submit" class="submit-btn" :disabled="isLoading">
          <span v-if="isLoading">로그인 중...</span>
          <span v-else>로그인</span>
        </button>
      </form>

      <!-- 회원가입 폼 -->
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
              {{ showPassword ? '👁️' : '🔒' }}
            </button>
          </div>
        </div>

        <div class="form-group">
          <label>회원 권한 구분</label>
          <select v-model="signUpForm.role">
            <option value="USER">일반 사용자 (USER)</option>
            <option value="ADMIN">관리자 (ADMIN)</option>
          </select>
        </div>

        <div v-if="errorMessage" class="error-banner">
          ⚠️ {{ errorMessage }}
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

    const data = await res.json()

    if (!res.ok) {
      throw new Error(data.message || '로그인에 실패했습니다.')
    }

    // 로그인 성공 시 토큰과 유저 정보 저장
    localStorage.setItem('accessToken', data.accessToken)
    localStorage.setItem('user', JSON.stringify(data.user))

    router.push('/')
  } catch (err: any) {
    errorMessage.value = err.message
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

    const data = await res.json()

    if (!res.ok) {
      throw new Error(data.message || '회원가입에 실패했습니다.')
    }

    alert('회원가입이 완료되었습니다! 로그인해 주세요.')
    isLoginMode.value = true
    loginForm.email = signUpForm.email
    loginForm.password = ''
  } catch (err: any) {
    errorMessage.value = err.message
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
  background-color: #0b0f19;
  font-family: 'Pretendard', -apple-system, BlinkMacSystemFont, system-ui, Roboto, sans-serif;
  padding: 24px;
}

.auth-card {
  width: 100%;
  max-width: 440px;
  background: #161f30;
  border-radius: 28px;
  padding: 40px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.4);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.brand-header {
  text-align: center;
  margin-bottom: 28px;
}

.brand-icon {
  font-size: 44px;
  margin-bottom: 8px;
}

.brand-title {
  color: #ffffff;
  font-size: 26px;
  font-weight: 800;
  letter-spacing: -0.5px;
  margin: 0 0 6px 0;
}

.brand-subtitle {
  color: #94a3b8;
  font-size: 14px;
  margin: 0;
}

.tab-switcher {
  display: flex;
  background: #0f172a;
  padding: 4px;
  border-radius: 16px;
  margin-bottom: 24px;
}

.tab-btn {
  flex: 1;
  padding: 12px;
  border: none;
  background: transparent;
  color: #64748b;
  font-size: 15px;
  font-weight: 600;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.tab-btn.active {
  background: #3182f6;
  color: #ffffff;
  box-shadow: 0 4px 12px rgba(49, 130, 246, 0.3);
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
  color: #cbd5e1;
  font-size: 13px;
  font-weight: 600;
}

.form-group input,
.form-group select {
  width: 100%;
  padding: 14px 16px;
  background: #0f172a;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 14px;
  color: #ffffff;
  font-size: 15px;
  outline: none;
  box-sizing: border-box;
  transition: border-color 0.2s;
}

.form-group input:focus,
.form-group select:focus {
  border-color: #3182f6;
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
  font-size: 16px;
  opacity: 0.7;
}

.eye-btn:hover {
  opacity: 1;
}

.error-banner {
  background: rgba(239, 68, 68, 0.15);
  border: 1px solid rgba(239, 68, 68, 0.4);
  color: #f87171;
  padding: 12px 14px;
  border-radius: 12px;
  font-size: 13px;
}

.submit-btn {
  margin-top: 10px;
  padding: 16px;
  background: #3182f6;
  color: #ffffff;
  border: none;
  border-radius: 16px;
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
  transition: background 0.2s, transform 0.1s;
}

.submit-btn:hover {
  background: #1b64d4;
}

.submit-btn:disabled {
  background: #475569;
  cursor: not-allowed;
}
</style>
