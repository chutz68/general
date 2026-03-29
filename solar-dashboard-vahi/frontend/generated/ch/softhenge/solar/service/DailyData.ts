interface DailyData {
    day?: string;
    scWh: number;
    bcWh: number;
    bdWh: number;
    socMax: number;
    socMin: number;
    tempRealMin: number;
    tempRealMax: number;
    rainAmountSum: number;
    selfConsumptionPct?: number;
    autarkyPct?: number;
    rowCount: number;
    missingRows: number;
    pwh: number;
    ewh: number;
    iwh: number;
    cwh: number;
    pwmax: number;
}
export default DailyData;
