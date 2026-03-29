import { _getPropertyModel as _getPropertyModel_1, makeObjectEmptyValueCreator as makeObjectEmptyValueCreator_1, NumberModel as NumberModel_1, ObjectModel as ObjectModel_1, StringModel as StringModel_1 } from "@vaadin/hilla-lit-form";
import type DailyData_1 from "./DailyData.js";
class DailyDataModel<T extends DailyData_1 = DailyData_1> extends ObjectModel_1<T> {
    static override createEmptyValue = makeObjectEmptyValueCreator_1(DailyDataModel);
    get day(): StringModel_1 {
        return this[_getPropertyModel_1]("day", (parent, key) => new StringModel_1(parent, key, true, { meta: { javaType: "java.lang.String" } }));
    }
    get scWh(): NumberModel_1 {
        return this[_getPropertyModel_1]("scWh", (parent, key) => new NumberModel_1(parent, key, false, { meta: { javaType: "double" } }));
    }
    get bcWh(): NumberModel_1 {
        return this[_getPropertyModel_1]("bcWh", (parent, key) => new NumberModel_1(parent, key, false, { meta: { javaType: "double" } }));
    }
    get bdWh(): NumberModel_1 {
        return this[_getPropertyModel_1]("bdWh", (parent, key) => new NumberModel_1(parent, key, false, { meta: { javaType: "double" } }));
    }
    get socMax(): NumberModel_1 {
        return this[_getPropertyModel_1]("socMax", (parent, key) => new NumberModel_1(parent, key, false, { meta: { javaType: "double" } }));
    }
    get socMin(): NumberModel_1 {
        return this[_getPropertyModel_1]("socMin", (parent, key) => new NumberModel_1(parent, key, false, { meta: { javaType: "double" } }));
    }
    get tempRealMin(): NumberModel_1 {
        return this[_getPropertyModel_1]("tempRealMin", (parent, key) => new NumberModel_1(parent, key, false, { meta: { javaType: "double" } }));
    }
    get tempRealMax(): NumberModel_1 {
        return this[_getPropertyModel_1]("tempRealMax", (parent, key) => new NumberModel_1(parent, key, false, { meta: { javaType: "double" } }));
    }
    get rainAmountSum(): NumberModel_1 {
        return this[_getPropertyModel_1]("rainAmountSum", (parent, key) => new NumberModel_1(parent, key, false, { meta: { javaType: "double" } }));
    }
    get selfConsumptionPct(): NumberModel_1 {
        return this[_getPropertyModel_1]("selfConsumptionPct", (parent, key) => new NumberModel_1(parent, key, true, { meta: { javaType: "java.lang.Double" } }));
    }
    get autarkyPct(): NumberModel_1 {
        return this[_getPropertyModel_1]("autarkyPct", (parent, key) => new NumberModel_1(parent, key, true, { meta: { javaType: "java.lang.Double" } }));
    }
    get rowCount(): NumberModel_1 {
        return this[_getPropertyModel_1]("rowCount", (parent, key) => new NumberModel_1(parent, key, false, { meta: { javaType: "int" } }));
    }
    get missingRows(): NumberModel_1 {
        return this[_getPropertyModel_1]("missingRows", (parent, key) => new NumberModel_1(parent, key, false, { meta: { javaType: "int" } }));
    }
    get pwh(): NumberModel_1 {
        return this[_getPropertyModel_1]("pwh", (parent, key) => new NumberModel_1(parent, key, false, { meta: { javaType: "double" } }));
    }
    get ewh(): NumberModel_1 {
        return this[_getPropertyModel_1]("ewh", (parent, key) => new NumberModel_1(parent, key, false, { meta: { javaType: "double" } }));
    }
    get iwh(): NumberModel_1 {
        return this[_getPropertyModel_1]("iwh", (parent, key) => new NumberModel_1(parent, key, false, { meta: { javaType: "double" } }));
    }
    get cwh(): NumberModel_1 {
        return this[_getPropertyModel_1]("cwh", (parent, key) => new NumberModel_1(parent, key, false, { meta: { javaType: "double" } }));
    }
    get pwmax(): NumberModel_1 {
        return this[_getPropertyModel_1]("pwmax", (parent, key) => new NumberModel_1(parent, key, false, { meta: { javaType: "double" } }));
    }
}
export default DailyDataModel;
