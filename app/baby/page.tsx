'use client'

import { useState } from 'react'
import { Heart, Sparkles, Gift, Stars, ArrowDown, MailOpen } from 'lucide-react'

const notes = [
  'You make ordinary days feel a little more magical. Never forget how special you are. 💗',
  'If I could send you one thing today, it would be a reminder that you deserve all the soft, happy things in life. 🌷',
  'Some people are sunshine. You are the whole golden hour. ✨',
]

export default function BabyPage() {
  const [opened, setOpened] = useState(false)
  const [surprise, setSurprise] = useState(false)
  const [noteIndex, setNoteIndex] = useState(0)

  const openNote = () => {
    setOpened(true)
    setNoteIndex((current) => (current + 1) % notes.length)
  }

  return (
    <main className="baby-page">
      <div className="baby-glow glow-one" />
      <div className="baby-glow glow-two" />
      <div className="baby-grain" aria-hidden="true" />

      <nav className="baby-nav">
        <a className="baby-brand" href="#home" aria-label="Little love home">
          <span className="brand-heart"><Heart size={17} fill="currentColor" /></span>
          <span>little <b>love.</b></span>
        </a>
        <span className="nav-note"><span className="live-dot" /> a little corner of happiness</span>
      </nav>

      <section className="hero" id="home">
        <div className="hero-copy">
          <div className="eyebrow"><Sparkles size={14} /> MADE WITH A LITTLE EXTRA LOVE</div>
          <h1>Hey, <span>baby.</span><br />You make life<br /><em>beautiful.</em></h1>
          <p className="hero-description">
            Just a tiny reminder that you are someone’s favourite notification,
            brightest thought, and sweetest hello.
          </p>
          <a className="scroll-link" href="#little-things">
            <span className="scroll-icon"><ArrowDown size={15} /></span>
            <span>there’s more for you</span>
          </a>
        </div>

        <div className="hero-art" aria-label="Decorative pink heart illustration">
          <div className="orbit orbit-a" />
          <div className="orbit orbit-b" />
          <span className="float-heart heart-a">♥</span>
          <span className="float-heart heart-b">♡</span>
          <span className="float-star star-a">✦</span>
          <span className="float-star star-b">✧</span>
          <div className="heart-halo">
            <div className="big-heart"><Heart size={112} strokeWidth={1.1} fill="currentColor" /></div>
          </div>
          <div className="tiny-note note-top">you are loved</div>
          <div className="tiny-note note-bottom">♡ always & forever ♡</div>
        </div>
      </section>

      <section className="little-things" id="little-things">
        <div className="section-heading">
          <span className="eyebrow"><Stars size={14} /> A FEW LITTLE THINGS</span>
          <h2>Because you deserve <em>the sweet stuff.</em></h2>
          <p>Small reminders. Big feelings. All for you.</p>
        </div>

        <div className="love-grid">
          <article className="love-card card-pink">
            <div className="card-icon"><Heart size={22} fill="currentColor" /></div>
            <span className="card-number">01 / A REMINDER</span>
            <h3>You are enough.</h3>
            <p>On your best days and your messy days. You never have to be anything other than you.</p>
            <span className="card-doodle">♡</span>
          </article>
          <article className="love-card card-peach">
            <div className="card-icon"><Sparkles size={22} /></div>
            <span className="card-number">02 / A WISH</span>
            <h3>More happy moments.</h3>
            <p>More belly laughs, warm hugs, unexpected surprises and reasons to smile.</p>
            <span className="card-doodle">✧</span>
          </article>
          <article className="love-card card-lilac">
            <div className="card-icon"><Gift size={22} /></div>
            <span className="card-number">03 / A LITTLE GIFT</span>
            <h3>A note just for you.</h3>
            <p>Tap the button below. I saved a little something inside this tiny envelope.</p>
            <span className="card-doodle">♡</span>
          </article>
        </div>
      </section>

      <section className="letter-section">
        <div className="letter-card">
          <div className="letter-sparkle sparkle-left">✧</div>
          <div className="letter-sparkle sparkle-right">✦</div>
          <div className="letter-icon"><MailOpen size={25} /></div>
          <span className="eyebrow">A LITTLE NOTE FOR YOU</span>
          <h2>{opened ? 'One more thing…' : 'Wanna open your surprise?'}</h2>
          <p className="letter-message">
            {opened ? notes[noteIndex] : 'There is a little bit of sweetness waiting inside. No special occasion needed.'}
          </p>
          <button className="surprise-button" onClick={openNote}>
            <Heart size={16} fill="currentColor" />
            {opened ? 'Another little note' : 'Open my surprise'}
          </button>
          {opened && <div className="opened-caption">made with ♡, especially for you</div>}
        </div>
      </section>

      <footer className="baby-footer">
        <div className="footer-heart"><Heart size={15} fill="currentColor" /></div>
        <p>made with love, just because.</p>
        <span>♡</span>
      </footer>

      {surprise && <div className="surprise-overlay" onClick={() => setSurprise(false)} />}
      <style jsx global>{`
        @import url('https://fonts.googleapis.com/css2?family=DM+Sans:wght@400;500;600;700&family=Playfair+Display:ital,wght@0,500;0,600;0,700;1,500;1,600&display=swap');
        * { box-sizing: border-box; }
        html { scroll-behavior: smooth; }
        body { margin: 0; }
        .baby-page { --ink: #49313c; --muted: #987d89; --pink: #ef8eae; position: relative; min-height: 100vh; overflow: hidden; color: var(--ink); background: #fff9f8; font-family: 'DM Sans', sans-serif; }
        .baby-glow { position: absolute; border-radius: 999px; filter: blur(2px); pointer-events: none; }
        .glow-one { width: 540px; height: 540px; top: -240px; right: -120px; background: radial-gradient(circle, #fbdce7 0%, #fbdce700 70%); }
        .glow-two { width: 500px; height: 500px; top: 700px; left: -270px; background: radial-gradient(circle, #ffe9df 0%, #ffe9df00 70%); }
        .baby-grain { position: absolute; inset: 0; opacity: .13; pointer-events: none; background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 180 180' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='.8' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)' opacity='.13'/%3E%3C/svg%3E"); }
        .baby-nav { max-width: 1160px; height: 82px; padding: 0 28px; margin: auto; display: flex; align-items: center; justify-content: space-between; position: relative; z-index: 2; }
        .baby-brand { display: inline-flex; align-items: center; gap: 10px; text-decoration: none; color: var(--ink); font-family: 'Playfair Display', serif; font-size: 22px; letter-spacing: -.7px; }
        .baby-brand b { color: #e47e9f; font-weight: 600; }
        .brand-heart { width: 34px; height: 34px; border-radius: 12px; display: grid; place-items: center; color: #fff; background: linear-gradient(145deg,#f3a9bf,#e77e9e); box-shadow: 0 5px 14px #e78aa744; }
        .nav-note { display: flex; align-items: center; gap: 8px; color: #a48c96; font-size: 11px; letter-spacing: .08em; text-transform: uppercase; }
        .live-dot { width: 6px; height: 6px; border-radius: 50%; background: #e58ba8; box-shadow: 0 0 0 4px #f9e2e9; }
        .hero { max-width: 1160px; min-height: 610px; padding: 70px 28px 100px; margin: auto; display: grid; grid-template-columns: 1fr 1fr; align-items: center; gap: 40px; position: relative; z-index: 1; }
        .hero-copy { padding-left: 5%; }
        .eyebrow { display: inline-flex; align-items: center; gap: 8px; color: #d77d9b; font-size: 10px; font-weight: 700; letter-spacing: .17em; }
        .hero h1 { margin: 24px 0 22px; font-family: 'Playfair Display', serif; font-size: clamp(58px,7vw,88px); font-weight: 500; line-height: .99; letter-spacing: -4px; }
        .hero h1 span { color: #e989a8; font-style: italic; }
        .hero h1 em { color: #b88cae; font-weight: 500; }
        .hero-description { max-width: 390px; color: #967d89; font-size: 15px; line-height: 1.9; }
        .scroll-link { margin-top: 36px; display: inline-flex; align-items: center; gap: 12px; color: #8f7581; text-decoration: none; font-size: 12px; font-weight: 600; }
        .scroll-icon { width: 32px; height: 32px; display: grid; place-items: center; border: 1px solid #efd4df; border-radius: 50%; color: #d77d9b; transition: .25s; }
        .scroll-link:hover .scroll-icon { transform: translateY(4px); background: #fae7ed; }
        .hero-art { min-height: 430px; position: relative; display: grid; place-items: center; }
        .heart-halo { width: 300px; height: 300px; border-radius: 50%; display: grid; place-items: center; background: radial-gradient(circle,#fff 0%,#fff3f6 48%,#fbe5ed 70%,#fbe5ed00 71%); animation: breathe 4s ease-in-out infinite; }
        .big-heart { color: #ed91ad; filter: drop-shadow(0 18px 18px #d9759945); animation: heartBeat 2.5s ease-in-out infinite; }
        .orbit { position: absolute; border: 1px solid #efcbd8; border-radius: 50%; transform: rotate(-22deg); }
        .orbit-a { width: 390px; height: 170px; }
        .orbit-b { width: 440px; height: 230px; transform: rotate(58deg); border-color: #f4dce3; }
        .float-heart,.float-star { position: absolute; z-index: 1; color: #e98eaa; }
        .heart-a { top: 66px; left: 22%; font-size: 32px; transform: rotate(-17deg); }
        .heart-b { right: 13%; bottom: 80px; font-size: 42px; color: #c49abd; }
        .star-a { top: 72px; right: 22%; font-size: 27px; color: #d5a5bf; }
        .star-b { bottom: 75px; left: 18%; font-size: 34px; color: #edb1bf; }
        .tiny-note { position: absolute; padding: 9px 15px; border: 1px solid #f4dce4; border-radius: 99px; background: #fffafbdc; color: #b27f94; font-family: 'Playfair Display',serif; font-style: italic; font-size: 13px; box-shadow: 0 8px 24px #c88da21a; }
        .note-top { top: 38px; right: 7%; transform: rotate(7deg); }
        .note-bottom { bottom: 32px; left: 8%; transform: rotate(-6deg); }
        .little-things { max-width: 1160px; padding: 65px 28px 105px; margin: auto; position: relative; z-index: 1; }
        .section-heading { text-align: center; }
        .section-heading h2 { margin: 17px 0 10px; font-family: 'Playfair Display',serif; font-weight: 500; font-size: clamp(32px,4vw,48px); letter-spacing: -1.5px; }
        .section-heading h2 em { color: #dc89a6; }
        .section-heading p { color: #a18a94; font-size: 13px; }
        .love-grid { display: grid; grid-template-columns: repeat(3,1fr); gap: 18px; margin-top: 48px; }
        .love-card { min-height: 245px; padding: 28px; border: 1px solid #f2e2e6; border-radius: 18px; position: relative; overflow: hidden; transition: transform .25s, box-shadow .25s; }
        .love-card:hover { transform: translateY(-6px); box-shadow: 0 18px 35px #9c64721a; }
        .card-pink { background: linear-gradient(145deg,#fff0f4,#fff9f8); }
        .card-peach { background: linear-gradient(145deg,#fff2e9,#fffaf7); }
        .card-lilac { background: linear-gradient(145deg,#f7effa,#fff9fb); }
        .card-icon { width: 43px; height: 43px; display: grid; place-items: center; border-radius: 14px; background: #fff; color: #df88a5; box-shadow: 0 5px 15px #bd84931c; }
        .card-number { display: block; margin-top: 23px; color: #bc91a0; font-size: 9px; font-weight: 700; letter-spacing: .15em; }
        .love-card h3 { margin: 9px 0; font-family: 'Playfair Display',serif; font-size: 23px; font-weight: 500; }
        .love-card p { max-width: 280px; color: #9c8490; font-size: 12px; line-height: 1.8; }
        .card-doodle { position: absolute; right: 22px; bottom: 4px; color: #e9b6c5; font-family: serif; font-size: 65px; opacity: .55; }
        .letter-section { padding: 25px 28px 115px; position: relative; z-index: 1; }
        .letter-card { max-width: 690px; min-height: 350px; margin: auto; padding: 52px 35px; border: 1px solid #f2dce4; border-radius: 24px; background: linear-gradient(145deg,#fff 0%,#fff3f6 100%); text-align: center; position: relative; overflow: hidden; box-shadow: 0 22px 65px #b97f9017; }
        .letter-icon { width: 52px; height: 52px; margin: 0 auto 18px; display: grid; place-items: center; border-radius: 17px; background: #fbe5ec; color: #d9809e; }
        .letter-card h2 { margin: 15px 0 10px; font-family: 'Playfair Display',serif; font-size: 34px; font-weight: 500; letter-spacing: -.8px; }
        .letter-message { max-width: 450px; min-height: 48px; margin: 0 auto 25px; color: #9b7e8b; font-size: 13px; line-height: 1.9; }
        .surprise-button { border: 0; border-radius: 99px; padding: 14px 23px; display: inline-flex; align-items: center; gap: 9px; background: linear-gradient(110deg,#e98eaa,#d58bb5); color: white; font: 600 12px 'DM Sans',sans-serif; cursor: pointer; box-shadow: 0 9px 20px #df91ae55; transition: transform .2s, box-shadow .2s; }
        .surprise-button:hover { transform: translateY(-3px); box-shadow: 0 13px 24px #df91ae66; }
        .opened-caption { margin-top: 16px; color: #c08ca0; font-family: 'Playfair Display',serif; font-size: 12px; font-style: italic; }
        .letter-sparkle { position: absolute; color: #e6b0c2; font-size: 30px; }
        .sparkle-left { top: 30px; left: 35px; }.sparkle-right { right: 38px; bottom: 28px; }
        .baby-footer { min-height: 85px; padding: 20px 28px; border-top: 1px solid #f3e4e8; display: flex; justify-content: center; align-items: center; gap: 11px; color: #a88d99; font-size: 11px; letter-spacing: .04em; position: relative; z-index: 1; }
        .baby-footer p { margin: 0; }.baby-footer span,.footer-heart { color: #e58aa8; }
        @keyframes breathe { 0%,100% { transform: scale(1); } 50% { transform: scale(1.035); } }
        @keyframes heartBeat { 0%,100% { transform: scale(1); } 12% { transform: scale(1.08); } 24% { transform: scale(1); } 36% { transform: scale(1.05); } }
        @media (max-width: 760px) {
          .baby-nav { height: 70px; padding: 0 21px; }.nav-note { font-size: 8px; letter-spacing: .04em; }
          .hero { padding: 48px 24px 70px; grid-template-columns: 1fr; gap: 12px; text-align: center; }
          .hero-copy { padding: 0; }.hero h1 { font-size: clamp(58px,15vw,78px); letter-spacing: -3px; margin-top: 20px; }
          .hero-description { margin: auto; font-size: 13px; }.scroll-link { margin-top: 24px; }
          .hero-art { min-height: 340px; margin-top: 5px; }.heart-halo { width: 245px; height: 245px; }.big-heart svg { width: 88px; height: 88px; }
          .orbit-a { width: 320px; height: 145px; }.orbit-b { width: 350px; height: 185px; }
          .note-top { right: 2%; top: 20px; }.note-bottom { left: 0; bottom: 20px; }
          .little-things { padding: 50px 22px 75px; }.section-heading h2 { font-size: 34px; }.love-grid { grid-template-columns: 1fr; gap: 13px; margin-top: 32px; }.love-card { min-height: 215px; }
          .letter-section { padding: 10px 20px 80px; }.letter-card { padding: 43px 22px; min-height: 340px; }.letter-card h2 { font-size: 29px; }
        }
        @media (prefers-reduced-motion: reduce) { *,*::before,*::after { animation-duration: .01ms !important; animation-iteration-count: 1 !important; scroll-behavior: auto !important; transition-duration: .01ms !important; } }
      `}</style>
    </main>
  )
}
