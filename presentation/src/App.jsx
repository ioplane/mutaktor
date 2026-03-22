import { useState, useEffect } from 'react'
import { AreaChart, Area, BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, PieChart, Pie, Cell, RadarChart, Radar, PolarGrid, PolarAngleAxis, PolarRadiusAxis } from 'recharts'
import { en, ru } from './i18n.js'

const COLORS = { accent: '#6c5ce7', accent2: '#a29bfe', green: '#00d68f', yellow: '#ffc107', red: '#ff6b6b', blue: '#4da6ff', orange: '#ff9f43', surface: '#12121a', bg: '#0a0a0f', text: '#e4e4ef' }

const revenueData = [
  { version: 'v1.0', arr: 12 },
  { version: 'v2.0', arr: 240 },
  { version: 'v2.2', arr: 510 },
  { version: 'v2.4', arr: 582 },
  { version: 'v3.0', arr: 882 },
]

const marketColors = [COLORS.accent2, COLORS.blue, COLORS.green]

const sprintData = [
  { sprint: 'S12', tests: 165 },
  { sprint: 'S16', tests: 220 },
  { sprint: 'S20', tests: 300 },
  { sprint: 'S22', tests: 350 },
  { sprint: 'S28', tests: 450 },
  { sprint: 'S32', tests: 550 },
  { sprint: 'S36', tests: 650 },
]

const pricingData = [
  { tier: 'CE', price: 0, color: COLORS.green },
  { tier: 'Pro', price: 15, color: COLORS.blue },
  { tier: 'Team', price: 20, color: COLORS.accent2 },
  { tier: 'Pro+', price: 30, color: COLORS.orange },
  { tier: 'Enterprise', price: 50, color: COLORS.red },
]

const SLIDES = 8

function LangSwitch({ t }) {
  return (
    <a href={t.switchPath} style={{
      position: 'fixed', top: 20, right: 24, zIndex: 200,
      padding: '6px 16px', borderRadius: 100, fontSize: '0.8rem', fontWeight: 700,
      background: 'rgba(108,92,231,0.2)', color: COLORS.accent2, border: `1px solid ${COLORS.accent}`,
      textDecoration: 'none', letterSpacing: '0.05em',
    }}>{t.switchLang}</a>
  )
}

function Nav({ active }) {
  return (
    <nav className="nav">
      {Array.from({ length: SLIDES }, (_, i) => (
        <button key={i} className={`nav-dot ${i === active ? 'active' : ''}`}
          onClick={() => document.getElementById(`slide-${i}`)?.scrollIntoView({ behavior: 'smooth' })} />
      ))}
    </nav>
  )
}

function SlideTitle({ t }) {
  return (
    <section className="slide" id="slide-0">
      <div>
        <div style={{ fontSize: '1rem', fontWeight: 600, color: COLORS.accent2, marginBottom: 16, letterSpacing: '0.1em', textTransform: 'uppercase' }}>{t.roadmap}</div>
        <h1><span className="gradient-text">Mutaktor</span></h1>
        <h1 style={{ fontSize: '2.2rem', fontWeight: 700, marginTop: 8, color: '#ccc' }}>{t.subtitle}</h1>
        <p className="subtitle">{t.tagline}</p>
        <div style={{ display: 'flex', gap: 12, marginTop: 40, flexWrap: 'wrap' }}>
          <span className="tag tag-green">Kotlin 2.3</span>
          <span className="tag tag-blue">Gradle 9.4</span>
          <span className="tag tag-purple">{t.tagZeroDeps}</span>
          <span className="tag tag-orange">Apache 2.0 + BSL</span>
        </div>
      </div>
    </section>
  )
}

function SlideMarket({ t }) {
  const marketData = [
    { name: t.marketPie[0], value: 4640 },
    { name: t.marketPie[1], value: 750 },
    { name: t.marketPie[2], value: 50 },
  ]
  return (
    <section className="slide" id="slide-1">
      <h2>{t.marketTitle[0]}<span className="gradient-text">{t.marketTitle[1]}</span></h2>
      <div className="grid-2">
        <div>
          <div className="grid-2" style={{ marginBottom: 24 }}>
            <div className="metric-card"><div className="value" style={{ color: COLORS.accent2 }}>$4.6B</div><div className="label">{t.aiTestingMarket}</div></div>
            <div className="metric-card"><div className="value" style={{ color: COLORS.green }}>18.3%</div><div className="label">{t.cagr}</div></div>
            <div className="metric-card"><div className="value" style={{ color: COLORS.blue }}>23M</div><div className="label">{t.javaDev}</div></div>
            <div className="metric-card"><div className="value" style={{ color: COLORS.orange }}>4.9M</div><div className="label">{t.kotlinDev}</div></div>
          </div>
          <div className="card" style={{ background: 'linear-gradient(135deg, rgba(108,92,231,0.1), rgba(0,214,143,0.1))' }}>
            <h3 style={{ color: COLORS.green }}>{t.keyInsight}</h3>
            <p style={{ fontSize: '0.95rem' }}>{t.keyInsightText[0]}<strong>{t.keyInsightText[1]}</strong>{t.keyInsightText[2]}</p>
          </div>
        </div>
        <div>
          <h3>{t.tamExpansion}</h3>
          <ResponsiveContainer width="100%" height={300}>
            <PieChart>
              <Pie data={marketData} cx="50%" cy="50%" outerRadius={120} dataKey="value" label={({ name }) => name} labelLine>
                {marketData.map((_, i) => <Cell key={i} fill={marketColors[i]} />)}
              </Pie>
              <Tooltip formatter={v => `$${v}M`} contentStyle={{ background: COLORS.surface, border: `1px solid ${COLORS.accent}`, borderRadius: 8 }} />
            </PieChart>
          </ResponsiveContainer>
        </div>
      </div>
    </section>
  )
}

function SlideCompetition({ t }) {
  const featureFlags = [
    [true, '$8/mo', false, false, false],
    [true, '$12/mo', false, false, false],
    [true, false, false, false, false],
    [true, false, false, false, false],
    [true, false, false, false, false],
    [true, false, false, false, false],
    [true, false, true, false, false],
    [true, false, false, false, true],
    [true, false, false, false, false],
    [true, false, true, false, false],
  ]
  return (
    <section className="slide" id="slide-2">
      <h2>{t.compTitle[0]}<span className="gradient-text">{t.compTitle[1]}</span></h2>
      <table className="comparison-table">
        <thead><tr>
          <th>{t.capability}</th>
          <th style={{ color: COLORS.green }}>Mutaktor</th>
          <th>ArcMutate</th><th>Develocity</th><th>Codecov</th><th>Trunk.io</th>
        </tr></thead>
        <tbody>
          {t.features.map((name, i) => (
            <tr key={i}>
              <td>{name}</td>
              {featureFlags[i].map((v, j) => (
                <td key={j}>{v === true ? <span className="check">&#10003;</span> : v === false ? <span className="cross">&#8212;</span> : <span style={{ color: COLORS.yellow }}>{v}</span>}</td>
              ))}
            </tr>
          ))}
          <tr style={{ fontWeight: 700 }}>
            <td>{t.price}</td>
            <td style={{ color: COLORS.green }}>$0-60/u/mo</td>
            <td>$8-12/u/mo</td><td>$35K/yr</td><td>$5-12/u/mo</td><td>Custom</td>
          </tr>
        </tbody>
      </table>
      <p style={{ marginTop: 16, fontSize: '0.9rem' }}>{t.compSummary[0]}<strong style={{ color: COLORS.green }}>{t.compSummary[1]}</strong>{t.compSummary[2]}</p>
    </section>
  )
}

function PhaseCard({ phase, num, color, versions, features }) {
  return (
    <div className="card" style={{ borderColor: color }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 16 }}>
        <div className="phase-number" style={{ background: `${color}22`, color }}>{num}</div>
        <div>
          <div style={{ fontWeight: 800, fontSize: '1.1rem' }}>{phase}</div>
          <div style={{ fontSize: '0.8rem', color: COLORS.accent2 }}>{versions}</div>
        </div>
      </div>
      {features.map((f, i) => (
        <div key={i} style={{ padding: '8px 0', borderTop: i > 0 ? `1px solid ${COLORS.surface}22` : 'none', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <span style={{ fontSize: '0.9rem' }}>{f.name}</span>
          <span className={`tag ${f.tag}`}>{f.version}</span>
        </div>
      ))}
    </div>
  )
}

function SlidePhases({ t }) {
  const vers = ['v0.2','v0.3','v0.3','v0.4','v0.5','v1.0','v1.1']
  const vers2 = ['v2.0','v2.0','v2.0','v2.2','v2.2','v2.4','v2.4']
  const vers3 = ['v3.0','v3.0','v3.0','v3.0','v3.0','v3.0']
  return (
    <section className="slide" id="slide-3">
      <h2>{t.phasesTitle[0]}<span className="gradient-text">{t.phasesTitle[1]}</span></h2>
      <div className="grid-3">
        <PhaseCard phase={t.phase1} num="1" color={COLORS.green} versions="v0.2 — v1.1" features={t.phase1Features.map((n,i) => ({ name: n, version: vers[i], tag: 'tag-green' }))} />
        <PhaseCard phase={t.phase2} num="2" color={COLORS.blue} versions="v2.0 — v2.4" features={t.phase2Features.map((n,i) => ({ name: n, version: vers2[i], tag: 'tag-blue' }))} />
        <PhaseCard phase={t.phase3} num="3" color={COLORS.accent2} versions="v3.0" features={t.phase3Features.map((n,i) => ({ name: n, version: vers3[i], tag: 'tag-purple' }))} />
      </div>
    </section>
  )
}

function SlideTQS({ t }) {
  const tqsRadarData = t.tqsMetrics.map((m, i) => ({ metric: m, weight: [25,20,15,15,10,10,5][i] }))
  const bars = t.tqsMetrics.map((m, i) => ({
    metric: m,
    value: [68,82,99,45,71,65,58][i],
    weight: ['0.25','0.20','0.15','0.15','0.10','0.10','0.05'][i],
    color: [COLORS.orange,COLORS.green,COLORS.green,COLORS.red,COLORS.yellow,COLORS.yellow,COLORS.yellow][i],
  }))
  return (
    <section className="slide" id="slide-4">
      <h2>{t.tqsTitle[0]}<span className="gradient-text">{t.tqsTitle[1]}</span></h2>
      <p className="subtitle" style={{ marginBottom: 40 }}>{t.tqsSubtitle}</p>
      <div className="grid-2">
        <div>
          <ResponsiveContainer width="100%" height={340}>
            <RadarChart data={tqsRadarData}>
              <PolarGrid stroke={COLORS.surface} />
              <PolarAngleAxis dataKey="metric" tick={{ fill: COLORS.accent2, fontSize: 11 }} />
              <PolarRadiusAxis angle={30} domain={[0, 30]} tick={false} axisLine={false} />
              <Radar dataKey="weight" stroke={COLORS.green} fill={COLORS.green} fillOpacity={0.25} strokeWidth={2} />
            </RadarChart>
          </ResponsiveContainer>
        </div>
        <div>
          <div className="card" style={{ marginBottom: 16 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span style={{ fontSize: '0.9rem', fontWeight: 600 }}>{t.tqsExample}</span>
              <span style={{ fontSize: '3rem', fontWeight: 900, color: COLORS.yellow }}>73</span>
            </div>
            <div style={{ height: 8, background: COLORS.surface, borderRadius: 4, marginTop: 12, overflow: 'hidden' }}>
              <div style={{ height: '100%', width: '73%', background: `linear-gradient(90deg, ${COLORS.red}, ${COLORS.yellow}, ${COLORS.green})`, borderRadius: 4 }} />
            </div>
          </div>
          {bars.map((m, i) => (
            <div key={i} className="bar-chart-row">
              <span className="bar-label">{m.metric}</span>
              <div className="bar-track">
                <div className="bar-fill" style={{ width: `${m.value}%`, background: m.color }}>{m.value}</div>
              </div>
              <span style={{ fontSize: '0.75rem', color: COLORS.accent2, width: 32 }}>{m.weight}</span>
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}

function SlideRevenue({ t }) {
  return (
    <section className="slide" id="slide-5">
      <h2>{t.revenueTitle[0]}<span className="gradient-text">{t.revenueTitle[1]}</span></h2>
      <div className="grid-2">
        <div>
          <ResponsiveContainer width="100%" height={360}>
            <AreaChart data={revenueData}>
              <CartesianGrid strokeDasharray="3 3" stroke="#1a1a28" />
              <XAxis dataKey="version" stroke={COLORS.accent2} />
              <YAxis stroke={COLORS.accent2} tickFormatter={v => `$${v}K`} />
              <Tooltip formatter={v => `$${v}K ARR`} contentStyle={{ background: COLORS.surface, border: `1px solid ${COLORS.accent}`, borderRadius: 8 }} />
              <defs>
                <linearGradient id="colorArr" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor={COLORS.green} stopOpacity={0.3} />
                  <stop offset="95%" stopColor={COLORS.green} stopOpacity={0} />
                </linearGradient>
              </defs>
              <Area type="monotone" dataKey="arr" stroke={COLORS.green} fill="url(#colorArr)" strokeWidth={3} />
            </AreaChart>
          </ResponsiveContainer>
        </div>
        <div>
          <h3>{t.pricingTiers}</h3>
          {pricingData.map((p, i) => (
            <div key={i} className="bar-chart-row">
              <span className="bar-label">{p.tier}</span>
              <div className="bar-track">
                <div className="bar-fill" style={{ width: `${Math.max(p.price * 1.6, 8)}%`, background: p.color }}>
                  {p.price === 0 ? t.free : `$${p.price}/u/mo`}
                </div>
              </div>
            </div>
          ))}
          <div style={{ marginTop: 32 }}>
            <div className="grid-2" style={{ gap: 16 }}>
              <div className="metric-card"><div className="value" style={{ color: COLORS.green, fontSize: '2rem' }}>$882K</div><div className="label">{t.targetArr}</div></div>
              <div className="metric-card"><div className="value" style={{ color: COLORS.blue, fontSize: '2rem' }}>20x</div><div className="label">{t.customerRoi}</div></div>
              <div className="metric-card"><div className="value" style={{ color: COLORS.orange, fontSize: '2rem' }}>6-14mo</div><div className="label">{t.breakEven}</div></div>
              <div className="metric-card"><div className="value" style={{ color: COLORS.accent2, fontSize: '2rem' }}>190+</div><div className="label">{t.payingClients}</div></div>
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}

function SlideSprints({ t }) {
  const items = [
    { s: '12-13', v: 'v0.2', color: COLORS.green },
    { s: '14-16', v: 'v0.3', color: COLORS.green },
    { s: '17-18', v: 'v0.4', color: COLORS.green },
    { s: '19-20', v: 'v0.5', color: COLORS.green },
    { s: '21-22', v: 'v1.0', color: COLORS.yellow },
    { s: '23-24', v: 'v1.1', color: COLORS.yellow },
    { s: '25-28', v: 'v2.0', color: COLORS.blue },
    { s: '29-30', v: 'v2.2', color: COLORS.blue },
    { s: '31-32', v: 'v2.4', color: COLORS.blue },
    { s: '33-36', v: 'v3.0', color: COLORS.accent2 },
  ]
  return (
    <section className="slide" id="slide-6">
      <h2>{t.sprintsTitle[0]}<span className="gradient-text">{t.sprintsTitle[1]}</span></h2>
      <div className="grid-2">
        <div className="timeline">
          {items.map((item, i) => (
            <div key={i} className="timeline-item" style={{ borderLeftColor: item.color }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                  <span style={{ fontWeight: 700 }}>{t.sprintItems[i]}</span>
                  <span style={{ color: COLORS.accent2, fontSize: '0.8rem', marginLeft: 8 }}>Sprint {item.s}</span>
                </div>
                <span className="tag" style={{ background: `${item.color}22`, color: item.color }}>{item.v}</span>
              </div>
            </div>
          ))}
        </div>
        <div>
          <h3>{t.testGrowth}</h3>
          <ResponsiveContainer width="100%" height={320}>
            <BarChart data={sprintData}>
              <CartesianGrid strokeDasharray="3 3" stroke="#1a1a28" />
              <XAxis dataKey="sprint" stroke={COLORS.accent2} />
              <YAxis stroke={COLORS.accent2} />
              <Tooltip contentStyle={{ background: COLORS.surface, border: `1px solid ${COLORS.accent}`, borderRadius: 8 }} />
              <Bar dataKey="tests" fill={COLORS.accent} radius={[6, 6, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
          <div style={{ marginTop: 24 }}>
            <h3>{t.architecture}</h3>
            <div className="card" style={{ fontFamily: 'monospace', fontSize: '0.8rem', lineHeight: 1.8, color: COLORS.accent2 }}>
              <div>{t.archLines[0]}</div>
              <div style={{ color: COLORS.text }}>{'  '}|</div>
              <div style={{ color: COLORS.green }}>{'  '}{t.archLines[1]}</div>
              <div style={{ color: COLORS.text }}>{'  '}{t.archLines[2]}</div>
              <div style={{ color: COLORS.text }}>{'  '}{t.archLines[3]}</div>
              <div style={{ color: COLORS.text }}>{'  '}|</div>
              <div style={{ color: COLORS.blue }}>{'  '}{t.archLines[4]}</div>
              <div style={{ color: COLORS.text }}>{'  '}{t.archLines[5]}</div>
              <div style={{ color: COLORS.text }}>{'  '}{t.archLines[6]}</div>
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}

function SlideSummary({ t }) {
  return (
    <section className="slide" id="slide-7">
      <h2>{t.summaryTitle[0]}<span className="gradient-text">{t.summaryTitle[1]}</span></h2>
      <div className="grid-4">
        {t.summaryCards.map((item, i) => (
          <div key={i} className="card" style={{ textAlign: 'center' }}>
            <div style={{ fontSize: '2.5rem', marginBottom: 12 }}>{item.icon}</div>
            <h3 style={{ fontSize: '1rem', color: COLORS.green }}>{item.title}</h3>
            <p style={{ fontSize: '0.85rem', color: COLORS.accent2 }}>{item.desc}</p>
          </div>
        ))}
      </div>
      <div style={{ textAlign: 'center', marginTop: 60 }}>
        <h1 style={{ fontSize: '2.5rem' }}><span className="gradient-text">github.com/ioplane/mutaktor</span></h1>
        <p style={{ color: COLORS.accent2, marginTop: 12, fontSize: '1.1rem' }}>{t.footerDesc}</p>
      </div>
    </section>
  )
}

export default function App() {
  const isRu = typeof window !== 'undefined' && window.location.pathname.startsWith('/ru')
  const t = isRu ? ru : en
  const [active, setActive] = useState(0)

  useEffect(() => {
    document.documentElement.lang = t.lang
    const observer = new IntersectionObserver(entries => {
      entries.forEach(e => {
        if (e.isIntersecting) setActive(parseInt(e.target.id.split('-')[1]))
      })
    }, { threshold: 0.4 })
    for (let i = 0; i < SLIDES; i++) {
      const el = document.getElementById(`slide-${i}`)
      if (el) observer.observe(el)
    }
    return () => observer.disconnect()
  }, [])

  return (
    <>
      <LangSwitch t={t} />
      <Nav active={active} />
      <SlideTitle t={t} />
      <SlideMarket t={t} />
      <SlideCompetition t={t} />
      <SlidePhases t={t} />
      <SlideTQS t={t} />
      <SlideRevenue t={t} />
      <SlideSprints t={t} />
      <SlideSummary t={t} />
    </>
  )
}
