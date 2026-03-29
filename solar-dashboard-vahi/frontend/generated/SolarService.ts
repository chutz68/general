import { EndpointRequestInit as EndpointRequestInit_1 } from "@vaadin/hilla-frontend";
import type DailyData_1 from "./ch/softhenge/solar/service/DailyData.js";
import type RecentData_1 from "./ch/softhenge/solar/service/RecentData.js";
import client_1 from "./connect-client.default.js";
async function getDailyData_1(fromDate: string | undefined, toDate: string | undefined, init?: EndpointRequestInit_1): Promise<Array<DailyData_1 | undefined> | undefined> { return client_1.call("SolarService", "getDailyData", { fromDate, toDate }, init); }
async function getRecentData_1(limit: number, init?: EndpointRequestInit_1): Promise<Array<RecentData_1 | undefined> | undefined> { return client_1.call("SolarService", "getRecentData", { limit }, init); }
async function getTodaySummary_1(init?: EndpointRequestInit_1): Promise<DailyData_1 | undefined> { return client_1.call("SolarService", "getTodaySummary", {}, init); }
export { getDailyData_1 as getDailyData, getRecentData_1 as getRecentData, getTodaySummary_1 as getTodaySummary };
