# Jike HotRank Dashboard

Vue 3 + Vite frontend for the Jike HotRank Engine demo dashboard.

## Commands

```bash
npm install
npm run dev
npm run build
```

Development server: `http://localhost:5173`

The Vite dev server proxies `/api` requests to the Spring Boot backend on `http://localhost:8080`.

Production build output goes to `../src/main/resources/static`, so the Spring Boot app can serve the SPA directly.

## Pages

- Overview: metrics, heat distribution, interaction stats, circle activity and cache gauge.
- Global ranking, circle ranking, newcomer ranking and surging ranking.
- Anti-spam report with blocked counts, suspicious users and invalid behavior charts.
- Interaction form for local demo writes.
