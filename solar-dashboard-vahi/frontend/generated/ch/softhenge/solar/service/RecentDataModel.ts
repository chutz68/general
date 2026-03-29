import { _getPropertyModel as _getPropertyModel_1, makeObjectEmptyValueCreator as makeObjectEmptyValueCreator_1, NumberModel as NumberModel_1, ObjectModel as ObjectModel_1, StringModel as StringModel_1 } from "@vaadin/hilla-lit-form";
import type RecentData_1 from "./RecentData.js";
class RecentDataModel<T extends RecentData_1 = RecentData_1> extends ObjectModel_1<T> {
    static override createEmptyValue = makeObjectEmptyValueCreator_1(RecentDataModel);
    get t(): StringModel_1 {
        return this[_getPropertyModel_1]("t", (parent, key) => new StringModel_1(parent, key, true, { meta: { javaType: "java.lang.String" } }));
    }
    get soc(): NumberModel_1 {
        return this[_getPropertyModel_1]("soc", (parent, key) => new NumberModel_1(parent, key, true, { meta: { javaType: "java.lang.Double" } }));
    }
    get tempReal(): NumberModel_1 {
        return this[_getPropertyModel_1]("tempReal", (parent, key) => new NumberModel_1(parent, key, true, { meta: { javaType: "java.lang.Double" } }));
    }
    get v(): NumberModel_1 {
        return this[_getPropertyModel_1]("v", (parent, key) => new NumberModel_1(parent, key, false, { meta: { javaType: "int" } }));
    }
    get thpWarmwaterC(): NumberModel_1 {
        return this[_getPropertyModel_1]("thpWarmwaterC", (parent, key) => new NumberModel_1(parent, key, true, { meta: { javaType: "java.lang.Double" } }));
    }
    get cw(): NumberModel_1 {
        return this[_getPropertyModel_1]("cw", (parent, key) => new NumberModel_1(parent, key, false, { meta: { javaType: "double" } }));
    }
    get pw(): NumberModel_1 {
        return this[_getPropertyModel_1]("pw", (parent, key) => new NumberModel_1(parent, key, false, { meta: { javaType: "double" } }));
    }
}
export default RecentDataModel;
