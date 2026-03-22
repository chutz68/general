import { useEffect, useState } from 'react';
import { SolarService } from 'Frontend/generated/endpoints';
import type DailyData from 'Frontend/generated/ch/softhenge/solar/service/DailyData';
import type RecentData from 'Frontend/generated/ch/softhenge/solar/service/RecentData';

export default function DashboardView() {
  const [dailyData, setDailyData] = useState<DailyData[]>([]);
  const [recentData, setRecentData] = useState<RecentData[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const today = new Date().toISOString().split('T')[0];
    const thirtyDaysAgo = new Date(Date.now() - 30 * 24 * 60 * 60 * 1000)
      .toISOString().split('T')[0];

    Promise.all([
      SolarService.getDailyData(thirtyDaysAgo, today),
      SolarService.getRecentData(288)
    ]).then(([daily, recent]) => {
      setDailyData((daily ?? []).filter((d): d is DailyData => d !== undefined));
      setRecentData((recent ?? []).filter((r): r is RecentData => r !== undefined));
      setLoading(false);
    });
  }, []);

  if (loading) return <div className="loading">Loading solar data...</div>;

  const today = dailyData[0];

  return (
    <div className="dashboard">
      {/* Today Summary */}
      <div className="summary-cards">
        <div className="card">
          <span className="label">Production</span>
          <span className="value">{((today?.pWh ?? 0) / 1000).toFixed(1)} kWh</span>
        </div>
        <div className="card">
          <span className="label">Consumption</span>
          <span className="value">{((today?.cWh ?? 0) / 1000).toFixed(1)} kWh</span>
        </div>
        <div className="card">
          <span className="label">Self Consumption</span>
          <span className="value">{today?.selfConsumptionPct?.toFixed(1) ?? '-'} %</span>
        </div>
        <div className="card">
          <span className="label">Autarky</span>
          <span className="value">{today?.autarkyPct?.toFixed(1) ?? '-'} %</span>
        </div>
        <div className="card">
          <span className="label">Export</span>
          <span className="value">{((today?.eWh ?? 0) / 1000).toFixed(1)} kWh</span>
        </div>
        <div className="card">
          <span className="label">Import</span>
          <span className="value">{((today?.iWh ?? 0) / 1000).toFixed(1)} kWh</span>
        </div>
      </div>

      {/* 30-day table */}
      <div className="table-section">
        <h2>Last 30 Days</h2>
        <table>
          <thead>
            <tr>
              <th>Day</th>
              <th>Production (kWh)</th>
              <th>Consumption (kWh)</th>
              <th>Export (kWh)</th>
              <th>Import (kWh)</th>
              <th>Self Consumption %</th>
              <th>Autarky %</th>
              <th>Temp Min/Max</th>
              <th>Quality</th>
            </tr>
          </thead>
          <tbody>
            {dailyData.map(row => (
              <tr key={row.day}>
                <td>{row.day}</td>
                <td>{((row.pWh ?? 0) / 1000).toFixed(1)}</td>
                <td>{((row.cWh ?? 0) / 1000).toFixed(1)}</td>
                <td>{((row.eWh ?? 0) / 1000).toFixed(1)}</td>
                <td>{((row.iWh ?? 0) / 1000).toFixed(1)}</td>
                <td>{row.selfConsumptionPct?.toFixed(1) ?? '-'}</td>
                <td>{row.autarkyPct?.toFixed(1) ?? '-'}</td>
                <td>{row.tempRealMin?.toFixed(1)} / {row.tempRealMax?.toFixed(1)} °C</td>
                <td>{row.rowCount === 288 ? '✅' : `⚠️ ${row.missingRows} missing`}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
