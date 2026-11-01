<template>
  <section class="auth-hero">
    <img
      class="auth-hero__photo"
      :src="xsImg"
      alt="杭州师范大学下沙校区荷塘"
      loading="eager"
    >
    <div class="auth-hero__veil" />

    <div class="auth-hero__inner">
      <div class="auth-hero__brand">
        <span class="auth-hero__crest">
          <img
            :src="hznuCrest"
            alt="杭州师范大学校徽"
          >
        </span>
        <span class="auth-hero__brand-text">
          <strong>杭州师范大学</strong>
          <span>智汇校园 · HZNU</span>
        </span>
      </div>

      <img
        class="auth-hero__motto"
        :src="hznuMottoLight"
        alt="校训：勤慎诚恕，博雅精进"
      >

      <div class="auth-hero__copy">
        <p class="auth-hero__eyebrow">
          HANGZHOU NORMAL UNIVERSITY · EST. 1908
        </p>
        <h1>
          湖光潋滟<br>
          <em>书香如故</em>
        </h1>
        <p class="auth-hero__lede">
          {{ resetMode ? '循着邮箱里的那封来信，重新回到你的校园。' : '在仓前与下沙之间，预约一段属于你的校园时光。' }}
        </p>
        <ul class="auth-hero__points">
          <li><span>壹</span>两校区服务，按仓前 / 下沙分流</li>
          <li><span>贰</span>咨询、教室、设备、活动一站办理</li>
          <li><span>叁</span>知识库 AI 助手，随时解答预约问题</li>
        </ul>
      </div>

      <div class="auth-hero__footer">
        <span>仓前 · 下沙</span>
        <span>CAMPUS APPOINTMENT × RAG ASSISTANT</span>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import xsImg from '@/assets/images/campus/xs-hero.jpg'
import hznuCrest from '@/assets/images/brand/hznu-crest.png'
import hznuMottoLight from '@/assets/images/brand/hznu-motto-light.png'

defineProps<{
  resetMode: boolean
}>()
</script>

<style scoped lang="scss">
.auth-hero {
  position: relative;
  display: flex;
  min-height: 620px;
  padding: 44px 48px;
  overflow: hidden;
  color: #f7f1e4;
  isolation: isolate;
}

.auth-hero__photo {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: 58% 50%;
  z-index: -2;
  filter: saturate(0.82) contrast(1.02);
}

.auth-hero__veil {
  position: absolute;
  inset: 0;
  z-index: -1;
  background:
    linear-gradient(100deg, rgba(13, 32, 39, 0.92) 0%, rgba(15, 38, 46, 0.76) 44%, rgba(15, 38, 46, 0.28) 78%, rgba(15, 38, 46, 0.1) 100%),
    linear-gradient(0deg, rgba(13, 32, 39, 0.55), transparent 45%);
}

.auth-hero::after {
  content: "";
  position: absolute;
  inset: 0;
  z-index: -1;
  pointer-events: none;
  opacity: 0.14;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='420' height='80' viewBox='0 0 420 80'%3E%3Cpath d='M0 40 Q 52.5 16 105 40 T 210 40 T 315 40 T 420 40' fill='none' stroke='%23d6c19a' stroke-width='1'/%3E%3C/svg%3E");
  background-size: 420px 80px;
  mask-image: linear-gradient(180deg, transparent 10%, #000 55%, transparent 95%);
}

.auth-hero__inner {
  position: relative;
  display: flex;
  flex-direction: column;
  width: 100%;
  min-width: 0;
}

.auth-hero__brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.auth-hero__seal {
  display: inline-grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border: 1.5px solid rgba(214, 193, 154, 0.72);
  border-radius: 3px;
  color: var(--gold-soft);
  background: rgba(247, 241, 228, 0.06);
  box-shadow: inset 0 0 0 3px rgba(13, 32, 39, 0.5), inset 0 0 0 4px rgba(214, 193, 154, 0.32);
  font-family: var(--font-serif);
  font-size: 20px;
  font-weight: 700;
  line-height: 1;
}

.auth-hero__crest {
  display: grid;
  place-items: center;
  width: 44px;
  height: 44px;
  flex-shrink: 0;
}

.auth-hero__crest img {
  width: 42px;
  height: 42px;
  object-fit: contain;
  filter: brightness(0) invert(1);
  opacity: 0.88;
}

.auth-hero__brand-text {
  display: grid;
  line-height: 1.2;
}

.auth-hero__brand-text strong {
  font-family: var(--font-serif);
  font-size: 17px;
  font-weight: 600;
  letter-spacing: 0.14em;
  color: #f7f1e4;
}

.auth-hero__brand-text span {
  margin-top: 4px;
  font-family: var(--font-latin);
  font-size: 8.5px;
  font-weight: 600;
  letter-spacing: 0.2em;
  color: rgba(247, 241, 228, 0.56);
}

.auth-hero__motto {
  position: absolute;
  top: 50%;
  right: 18px;
  width: auto;
  height: 190px;
  transform: translateY(-50%);
  opacity: 0.62;
  filter: drop-shadow(0 8px 24px rgba(13, 32, 39, 0.3));
  pointer-events: none;
}

.auth-hero__copy {
  margin-top: auto;
  padding: 56px 0 36px;
  max-width: 520px;
}

.auth-hero__eyebrow {
  margin: 0 0 18px;
  font-family: var(--font-latin);
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.22em;
  color: var(--gold-soft);
}

.auth-hero h1 {
  margin: 0 0 20px;
  font-family: var(--font-serif);
  font-size: clamp(42px, 4.4vw, 58px);
  font-weight: 600;
  line-height: 1.22;
  letter-spacing: 0.04em;
  color: #f8f2e6;
}

.auth-hero h1 em {
  font-style: normal;
  color: var(--gold-soft);
}

.auth-hero__lede {
  margin: 0;
  max-width: 420px;
  font-size: 14px;
  line-height: 1.9;
  color: rgba(247, 241, 228, 0.78);
}

.auth-hero__points {
  display: grid;
  gap: 12px;
  margin: 32px 0 0;
  padding: 0;
  list-style: none;
}

.auth-hero__points li {
  display: flex;
  align-items: center;
  gap: 11px;
  font-size: 12.5px;
  letter-spacing: 0.03em;
  color: rgba(247, 241, 228, 0.78);
}

.auth-hero__points span {
  display: inline-grid;
  place-items: center;
  width: 20px;
  height: 20px;
  border: 1px solid rgba(214, 193, 154, 0.34);
  border-radius: 50%;
  font-family: var(--font-latin);
  font-size: 9px;
  color: var(--gold-soft);
  flex-shrink: 0;
}

.auth-hero__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: auto;
  padding-top: 22px;
  border-top: 1px solid rgba(247, 241, 228, 0.16);
  font-family: var(--font-latin);
  font-size: 8.5px;
  letter-spacing: 0.16em;
  color: rgba(247, 241, 228, 0.46);
}

@media (max-width: 1100px) {
  .auth-hero {
    padding: 34px 36px;
    min-height: 420px;
  }

  .auth-hero__motto {
    display: none;
  }

  .auth-hero__copy {
    padding: 36px 0 20px;
  }

  .auth-hero__points,
  .auth-hero__footer {
    display: none;
  }

  .auth-hero h1 {
    font-size: clamp(32px, 5vw, 42px);
  }
}

@media (max-width: 700px) {
  .auth-hero {
    min-height: 260px;
    padding: 24px;
  }

  .auth-hero__lede {
    font-size: 13px;
  }

  .auth-hero__copy {
    padding: 26px 0 0;
  }
}
</style>
