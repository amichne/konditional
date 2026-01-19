import { jsx as r, jsxs as c } from "react/jsx-runtime";
import { createContext as xe, useMemo as y, useCallback as u, useContext as Pe, useReducer as Be, memo as Ee, useState as T } from "react";
var Se = /* @__PURE__ */ Symbol.for("immer-nothing"), Ne = /* @__PURE__ */ Symbol.for("immer-draftable"), A = /* @__PURE__ */ Symbol.for("immer-state"), Ve = process.env.NODE_ENV !== "production" ? [
  // All error codes, starting by 0:
  function(e) {
    return `The plugin for '${e}' has not been loaded into Immer. To enable the plugin, import and call \`enable${e}()\` when initializing your application.`;
  },
  function(e) {
    return `produce can only be called on things that are draftable: plain objects, arrays, Map, Set or classes that are marked with '[immerable]: true'. Got '${e}'`;
  },
  "This object has been frozen and should not be mutated",
  function(e) {
    return "Cannot use a proxy that has been revoked. Did you pass an object from inside an immer function to an async process? " + e;
  },
  "An immer producer returned a new value *and* modified its draft. Either return a new value *or* modify the draft.",
  "Immer forbids circular references",
  "The first or second argument to `produce` must be a function",
  "The third argument to `produce` must be a function or undefined",
  "First argument to `createDraft` must be a plain object, an array, or an immerable object",
  "First argument to `finishDraft` must be a draft returned by `createDraft`",
  function(e) {
    return `'current' expects a draft, got: ${e}`;
  },
  "Object.defineProperty() cannot be used on an Immer draft",
  "Object.setPrototypeOf() cannot be used on an Immer draft",
  "Immer only supports deleting array indices",
  "Immer only supports setting array indices and the 'length' property",
  function(e) {
    return `'original' expects a draft, got: ${e}`;
  }
  // Note: if more errors are added, the errorOffset in Patches.ts should be increased
  // See Patches.ts for additional errors
] : [];
function _(e, ...t) {
  if (process.env.NODE_ENV !== "production") {
    const a = Ve[e], n = typeof a == "function" ? a.apply(null, t) : a;
    throw new Error(`[Immer] ${n}`);
  }
  throw new Error(
    `[Immer] minified error nr: ${e}. Full error at: https://bit.ly/3cXEKWf`
  );
}
var G = Object.getPrototypeOf;
function V(e) {
  return !!e && !!e[A];
}
function F(e) {
  return e ? be(e) || Array.isArray(e) || !!e[Ne] || !!e.constructor?.[Ne] || H(e) || te(e) : !1;
}
var je = Object.prototype.constructor.toString(), ge = /* @__PURE__ */ new WeakMap();
function be(e) {
  if (!e || typeof e != "object")
    return !1;
  const t = Object.getPrototypeOf(e);
  if (t === null || t === Object.prototype)
    return !0;
  const a = Object.hasOwnProperty.call(t, "constructor") && t.constructor;
  if (a === Object)
    return !0;
  if (typeof a != "function")
    return !1;
  let n = ge.get(a);
  return n === void 0 && (n = Function.toString.call(a), ge.set(a, n)), n === je;
}
function Y(e, t, a = !0) {
  ee(e) === 0 ? (a ? Reflect.ownKeys(e) : Object.keys(e)).forEach((s) => {
    t(s, e[s], e);
  }) : e.forEach((n, s) => t(s, n, e));
}
function ee(e) {
  const t = e[A];
  return t ? t.type_ : Array.isArray(e) ? 1 : H(e) ? 2 : te(e) ? 3 : 0;
}
function ie(e, t) {
  return ee(e) === 2 ? e.has(t) : Object.prototype.hasOwnProperty.call(e, t);
}
function Ue(e, t, a) {
  const n = ee(e);
  n === 2 ? e.set(t, a) : n === 3 ? e.add(a) : e[t] = a;
}
function Ge(e, t) {
  return e === t ? e !== 0 || 1 / e === 1 / t : e !== e && t !== t;
}
function H(e) {
  return e instanceof Map;
}
function te(e) {
  return e instanceof Set;
}
function M(e) {
  return e.copy_ || e.base_;
}
function le(e, t) {
  if (H(e))
    return new Map(e);
  if (te(e))
    return new Set(e);
  if (Array.isArray(e))
    return Array.prototype.slice.call(e);
  const a = be(e);
  if (t === !0 || t === "class_only" && !a) {
    const n = Object.getOwnPropertyDescriptors(e);
    delete n[A];
    let s = Reflect.ownKeys(n);
    for (let l = 0; l < s.length; l++) {
      const i = s[l], o = n[i];
      o.writable === !1 && (o.writable = !0, o.configurable = !0), (o.get || o.set) && (n[i] = {
        configurable: !0,
        writable: !0,
        // could live with !!desc.set as well here...
        enumerable: o.enumerable,
        value: e[i]
      });
    }
    return Object.create(G(e), n);
  } else {
    const n = G(e);
    if (n !== null && a)
      return { ...e };
    const s = Object.create(n);
    return Object.assign(s, e);
  }
}
function me(e, t = !1) {
  return ae(e) || V(e) || !F(e) || (ee(e) > 1 && Object.defineProperties(e, {
    set: K,
    add: K,
    clear: K,
    delete: K
  }), Object.freeze(e), t && Object.values(e).forEach((a) => me(a, !0))), e;
}
function ze() {
  _(2);
}
var K = {
  value: ze
};
function ae(e) {
  return e === null || typeof e != "object" ? !0 : Object.isFrozen(e);
}
var Xe = {};
function $(e) {
  const t = Xe[e];
  return t || _(0, e), t;
}
var z;
function we() {
  return z;
}
function We(e, t) {
  return {
    drafts_: [],
    parent_: e,
    immer_: t,
    // Whenever the modified draft contains a draft from another scope, we
    // need to prevent auto-freezing so the unowned draft can be finalized.
    canAutoFreeze_: !0,
    unfinalizedDrafts_: 0
  };
}
function ke(e, t) {
  t && ($("Patches"), e.patches_ = [], e.inversePatches_ = [], e.patchListener_ = t);
}
function ce(e) {
  oe(e), e.drafts_.forEach(He), e.drafts_ = null;
}
function oe(e) {
  e === z && (z = e.parent_);
}
function ye(e) {
  return z = We(z, e);
}
function He(e) {
  const t = e[A];
  t.type_ === 0 || t.type_ === 1 ? t.revoke_() : t.revoked_ = !0;
}
function _e(e, t) {
  t.unfinalizedDrafts_ = t.drafts_.length;
  const a = t.drafts_[0];
  return e !== void 0 && e !== a ? (a[A].modified_ && (ce(t), _(4)), F(e) && (e = Z(t, e), t.parent_ || Q(t, e)), t.patches_ && $("Patches").generateReplacementPatches_(
    a[A].base_,
    e,
    t.patches_,
    t.inversePatches_
  )) : e = Z(t, a, []), ce(t), t.patches_ && t.patchListener_(t.patches_, t.inversePatches_), e !== Se ? e : void 0;
}
function Z(e, t, a) {
  if (ae(t))
    return t;
  const n = e.immer_.shouldUseStrictIteration(), s = t[A];
  if (!s)
    return Y(
      t,
      (l, i) => ve(e, s, t, l, i, a),
      n
    ), t;
  if (s.scope_ !== e)
    return t;
  if (!s.modified_)
    return Q(e, s.base_, !0), s.base_;
  if (!s.finalized_) {
    s.finalized_ = !0, s.scope_.unfinalizedDrafts_--;
    const l = s.copy_;
    let i = l, o = !1;
    s.type_ === 3 && (i = new Set(l), l.clear(), o = !0), Y(
      i,
      (p, N) => ve(
        e,
        s,
        l,
        p,
        N,
        a,
        o
      ),
      n
    ), Q(e, l, !1), a && e.patches_ && $("Patches").generatePatches_(
      s,
      a,
      e.patches_,
      e.inversePatches_
    );
  }
  return s.copy_;
}
function ve(e, t, a, n, s, l, i) {
  if (s == null || typeof s != "object" && !i)
    return;
  const o = ae(s);
  if (!(o && !i)) {
    if (process.env.NODE_ENV !== "production" && s === a && _(5), V(s)) {
      const p = l && t && t.type_ !== 3 && // Set objects are atomic since they have no keys.
      !ie(t.assigned_, n) ? l.concat(n) : void 0, N = Z(e, s, p);
      if (Ue(a, n, N), V(N))
        e.canAutoFreeze_ = !1;
      else
        return;
    } else i && a.add(s);
    if (F(s) && !o) {
      if (!e.immer_.autoFreeze_ && e.unfinalizedDrafts_ < 1 || t && t.base_ && t.base_[n] === s && o)
        return;
      Z(e, s), (!t || !t.scope_.parent_) && typeof n != "symbol" && (H(a) ? a.has(n) : Object.prototype.propertyIsEnumerable.call(a, n)) && Q(e, s);
    }
  }
}
function Q(e, t, a = !1) {
  !e.parent_ && e.immer_.autoFreeze_ && e.canAutoFreeze_ && me(t, a);
}
function Je(e, t) {
  const a = Array.isArray(e), n = {
    type_: a ? 1 : 0,
    // Track which produce call this is associated with.
    scope_: t ? t.scope_ : we(),
    // True for both shallow and deep changes.
    modified_: !1,
    // Used during finalization.
    finalized_: !1,
    // Track which properties have been assigned (true) or deleted (false).
    assigned_: {},
    // The parent draft state.
    parent_: t,
    // The base state.
    base_: e,
    // The base proxy.
    draft_: null,
    // set below
    // The base copy with any updated values.
    copy_: null,
    // Called by the `produce` function.
    revoke_: null,
    isManual_: !1
  };
  let s = n, l = he;
  a && (s = [n], l = X);
  const { revoke: i, proxy: o } = Proxy.revocable(s, l);
  return n.draft_ = o, n.revoke_ = i, o;
}
var he = {
  get(e, t) {
    if (t === A)
      return e;
    const a = M(e);
    if (!ie(a, t))
      return Ke(e, a, t);
    const n = a[t];
    return e.finalized_ || !F(n) ? n : n === se(e.base_, t) ? (re(e), e.copy_[t] = de(n, e)) : n;
  },
  has(e, t) {
    return t in M(e);
  },
  ownKeys(e) {
    return Reflect.ownKeys(M(e));
  },
  set(e, t, a) {
    const n = Ce(M(e), t);
    if (n?.set)
      return n.set.call(e.draft_, a), !0;
    if (!e.modified_) {
      const s = se(M(e), t), l = s?.[A];
      if (l && l.base_ === a)
        return e.copy_[t] = a, e.assigned_[t] = !1, !0;
      if (Ge(a, s) && (a !== void 0 || ie(e.base_, t)))
        return !0;
      re(e), ue(e);
    }
    return e.copy_[t] === a && // special case: handle new props with value 'undefined'
    (a !== void 0 || t in e.copy_) || // special case: NaN
    Number.isNaN(a) && Number.isNaN(e.copy_[t]) || (e.copy_[t] = a, e.assigned_[t] = !0), !0;
  },
  deleteProperty(e, t) {
    return se(e.base_, t) !== void 0 || t in e.base_ ? (e.assigned_[t] = !1, re(e), ue(e)) : delete e.assigned_[t], e.copy_ && delete e.copy_[t], !0;
  },
  // Note: We never coerce `desc.value` into an Immer draft, because we can't make
  // the same guarantee in ES5 mode.
  getOwnPropertyDescriptor(e, t) {
    const a = M(e), n = Reflect.getOwnPropertyDescriptor(a, t);
    return n && {
      writable: !0,
      configurable: e.type_ !== 1 || t !== "length",
      enumerable: n.enumerable,
      value: a[t]
    };
  },
  defineProperty() {
    _(11);
  },
  getPrototypeOf(e) {
    return G(e.base_);
  },
  setPrototypeOf() {
    _(12);
  }
}, X = {};
Y(he, (e, t) => {
  X[e] = function() {
    return arguments[0] = arguments[0][0], t.apply(this, arguments);
  };
});
X.deleteProperty = function(e, t) {
  return process.env.NODE_ENV !== "production" && isNaN(parseInt(t)) && _(13), X.set.call(this, e, t, void 0);
};
X.set = function(e, t, a) {
  return process.env.NODE_ENV !== "production" && t !== "length" && isNaN(parseInt(t)) && _(14), he.set.call(this, e[0], t, a, e[0]);
};
function se(e, t) {
  const a = e[A];
  return (a ? M(a) : e)[t];
}
function Ke(e, t, a) {
  const n = Ce(t, a);
  return n ? "value" in n ? n.value : (
    // This is a very special case, if the prop is a getter defined by the
    // prototype, we should invoke it with the draft as context!
    n.get?.call(e.draft_)
  ) : void 0;
}
function Ce(e, t) {
  if (!(t in e))
    return;
  let a = G(e);
  for (; a; ) {
    const n = Object.getOwnPropertyDescriptor(a, t);
    if (n)
      return n;
    a = G(a);
  }
}
function ue(e) {
  e.modified_ || (e.modified_ = !0, e.parent_ && ue(e.parent_));
}
function re(e) {
  e.copy_ || (e.copy_ = le(
    e.base_,
    e.scope_.immer_.useStrictShallowCopy_
  ));
}
var qe = class {
  constructor(e) {
    this.autoFreeze_ = !0, this.useStrictShallowCopy_ = !1, this.useStrictIteration_ = !0, this.produce = (t, a, n) => {
      if (typeof t == "function" && typeof a != "function") {
        const l = a;
        a = t;
        const i = this;
        return function(p = l, ...N) {
          return i.produce(p, (S) => a.call(this, S, ...N));
        };
      }
      typeof a != "function" && _(6), n !== void 0 && typeof n != "function" && _(7);
      let s;
      if (F(t)) {
        const l = ye(this), i = de(t, void 0);
        let o = !0;
        try {
          s = a(i), o = !1;
        } finally {
          o ? ce(l) : oe(l);
        }
        return ke(l, n), _e(s, l);
      } else if (!t || typeof t != "object") {
        if (s = a(t), s === void 0 && (s = t), s === Se && (s = void 0), this.autoFreeze_ && me(s, !0), n) {
          const l = [], i = [];
          $("Patches").generateReplacementPatches_(t, s, l, i), n(l, i);
        }
        return s;
      } else
        _(1, t);
    }, this.produceWithPatches = (t, a) => {
      if (typeof t == "function")
        return (i, ...o) => this.produceWithPatches(i, (p) => t(p, ...o));
      let n, s;
      return [this.produce(t, a, (i, o) => {
        n = i, s = o;
      }), n, s];
    }, typeof e?.autoFreeze == "boolean" && this.setAutoFreeze(e.autoFreeze), typeof e?.useStrictShallowCopy == "boolean" && this.setUseStrictShallowCopy(e.useStrictShallowCopy), typeof e?.useStrictIteration == "boolean" && this.setUseStrictIteration(e.useStrictIteration);
  }
  createDraft(e) {
    F(e) || _(8), V(e) && (e = Ye(e));
    const t = ye(this), a = de(e, void 0);
    return a[A].isManual_ = !0, oe(t), a;
  }
  finishDraft(e, t) {
    const a = e && e[A];
    (!a || !a.isManual_) && _(9);
    const { scope_: n } = a;
    return ke(n, t), _e(void 0, n);
  }
  /**
   * Pass true to automatically freeze all copies created by Immer.
   *
   * By default, auto-freezing is enabled.
   */
  setAutoFreeze(e) {
    this.autoFreeze_ = e;
  }
  /**
   * Pass true to enable strict shallow copy.
   *
   * By default, immer does not copy the object descriptors such as getter, setter and non-enumrable properties.
   */
  setUseStrictShallowCopy(e) {
    this.useStrictShallowCopy_ = e;
  }
  /**
   * Pass false to use faster iteration that skips non-enumerable properties
   * but still handles symbols for compatibility.
   *
   * By default, strict iteration is enabled (includes all own properties).
   */
  setUseStrictIteration(e) {
    this.useStrictIteration_ = e;
  }
  shouldUseStrictIteration() {
    return this.useStrictIteration_;
  }
  applyPatches(e, t) {
    let a;
    for (a = t.length - 1; a >= 0; a--) {
      const s = t[a];
      if (s.path.length === 0 && s.op === "replace") {
        e = s.value;
        break;
      }
    }
    a > -1 && (t = t.slice(a + 1));
    const n = $("Patches").applyPatches_;
    return V(e) ? n(e, t) : this.produce(
      e,
      (s) => n(s, t)
    );
  }
};
function de(e, t) {
  const a = H(e) ? $("MapSet").proxyMap_(e, t) : te(e) ? $("MapSet").proxySet_(e, t) : Je(e, t);
  return (t ? t.scope_ : we()).drafts_.push(a), a;
}
function Ye(e) {
  return V(e) || _(10, e), De(e);
}
function De(e) {
  if (!F(e) || ae(e))
    return e;
  const t = e[A];
  let a, n = !0;
  if (t) {
    if (!t.modified_)
      return t.base_;
    t.finalized_ = !0, a = le(e, t.scope_.immer_.useStrictShallowCopy_), n = t.scope_.immer_.shouldUseStrictIteration();
  } else
    a = le(e, !0);
  return Y(
    a,
    (s, l) => {
      Ue(a, s, De(l));
    },
    n
  ), t && (t.finalized_ = !1), a;
}
var Ze = new qe(), Qe = Ze.produce;
function v(e, t, a, n) {
  e.issues.push({
    severity: t,
    code: a,
    message: n,
    path: [...e.currentPath]
  });
}
function g(e, t, a) {
  e.currentPath.push(t);
  try {
    return a();
  } finally {
    e.currentPath.pop();
  }
}
function et(e, t) {
  return e.major !== t.major ? e.major - t.major : e.minor !== t.minor ? e.minor - t.minor : e.patch - t.patch;
}
function tt(e, t) {
  t.type === "MIN_AND_MAX_BOUND" && et(t.min, t.max) > 0 && v(
    e,
    "error",
    "VERSION_MIN_EXCEEDS_MAX",
    `Version minimum (${t.min.major}.${t.min.minor}.${t.min.patch}) exceeds maximum (${t.max.major}.${t.max.minor}.${t.max.patch})`
  );
}
function at(e, t) {
  (t < 0 || t > 100) && v(
    e,
    "error",
    "RAMPUP_OUT_OF_BOUNDS",
    `Ramp-up percentage ${t} is outside valid range [0, 100]`
  );
}
function Oe(e, t) {
  /^[0-9a-fA-F]+$/.test(t) || v(
    e,
    "error",
    "INVALID_HEX_STABLE_ID",
    `Allowlist entry "${t}" is not valid hexadecimal`
  );
}
function nt(e, t) {
  if (!e.schema) return;
  const a = e.schema.enums[t.enumClassName];
  if (!a) {
    v(
      e,
      "error",
      "ENUM_UNKNOWN_CLASS",
      `Unknown enum class "${t.enumClassName}"`
    );
    return;
  }
  a.includes(t.value) || v(
    e,
    "error",
    "UNKNOWN_ENUM_VALUE",
    `"${t.value}" is not a valid value for enum ${t.enumClassName}. Valid values: ${a.join(", ")}`
  );
}
function st(e, t) {
  if (!e.schema) return;
  const a = e.schema.dataClasses[t.dataClassName];
  if (!a) {
    v(
      e,
      "error",
      "DATACLASS_UNKNOWN_CLASS",
      `Unknown data class "${t.dataClassName}"`
    );
    return;
  }
  rt(e, t.value, a);
}
function rt(e, t, a) {
  for (const n of a.required)
    n in t || g(e, n, () => {
      v(
        e,
        "error",
        "DATACLASS_MISSING_REQUIRED_FIELD",
        `Required field "${n}" is missing`
      );
    });
  for (const [n, s] of Object.entries(t))
    g(e, n, () => {
      const l = a.properties[n];
      if (!l) {
        v(
          e,
          "warning",
          "DATACLASS_UNKNOWN_FIELD",
          `Field "${n}" is not defined in schema (will be ignored)`
        );
        return;
      }
      const i = it(s);
      lt(i, l.type) || v(
        e,
        "error",
        "DATACLASS_FIELD_TYPE_MISMATCH",
        `Field "${n}" has type ${i}, expected ${l.type}`
      );
    });
}
function it(e) {
  return e === null ? "null" : Array.isArray(e) ? "array" : typeof e == "number" ? Number.isInteger(e) ? "integer" : "number" : typeof e;
}
function lt(e, t) {
  return e === t || e === "integer" && t === "number";
}
function Ie(e, t) {
  switch (t.type) {
    case "ENUM":
      nt(e, t);
      break;
    case "DATA_CLASS":
      st(e, t);
      break;
  }
}
function ct(e, t, a) {
  g(e, a, () => {
    g(e, "platforms", () => {
      t.platforms.length === 0 && v(
        e,
        "error",
        "EMPTY_PLATFORM_SET",
        "Rule has empty platform set and can never match"
      );
    }), g(e, "locales", () => {
      (Array.isArray(t.locales) ? t.locales : [t.locales]).length === 0 && v(
        e,
        "error",
        "EMPTY_LOCALE_SET",
        "Rule has empty locale set and can never match"
      );
    }), g(e, "versionRange", () => {
      tt(e, t.versionRange);
    }), g(e, "rampUp", () => {
      at(e, t.rampUp);
    }), t.rampUp === 0 && t.rampUpAllowlist.length === 0 && v(
      e,
      "warning",
      "RAMPUP_ZERO_NO_ALLOWLIST",
      "Rule has 0% ramp-up with no allowlist entries and is effectively disabled"
    ), g(e, "rampUpAllowlist", () => {
      t.rampUpAllowlist.forEach((n, s) => {
        g(e, s, () => {
          Oe(e, n);
        });
      });
    }), g(e, "value", () => {
      Ie(e, t.value);
    });
  });
}
function ot(e, t, a) {
  g(e, a, () => {
    !t.isActive && t.rules.length > 0 && v(
      e,
      "warning",
      "FLAG_INACTIVE_WITH_RULES",
      `Flag "${t.key}" is inactive but has ${t.rules.length} rule(s) that will never evaluate`
    ), g(e, "defaultValue", () => {
      Ie(e, t.defaultValue);
    }), g(e, "rampUpAllowlist", () => {
      t.rampUpAllowlist.forEach((n, s) => {
        g(e, s, () => {
          Oe(e, n);
        });
      });
    }), g(e, "rules", () => {
      t.rules.forEach((n, s) => {
        ct(e, n, s);
      }), ut(e, t.rules);
    });
  });
}
function ut(e, t) {
  const a = /* @__PURE__ */ new Map();
  t.forEach((n, s) => {
    const l = dt(n), i = a.get(l);
    i !== void 0 ? g(e, s, () => {
      v(
        e,
        "warning",
        "DUPLICATE_RULE",
        `Rule is identical to rule at index ${i}`
      );
    }) : a.set(l, s);
  });
}
function dt(e) {
  const t = {
    platforms: [...e.platforms].sort(),
    locales: Array.isArray(e.locales) ? [...e.locales].sort() : [e.locales],
    versionRange: e.versionRange,
    axes: Object.fromEntries(
      Object.entries(e.axes).sort(([a], [n]) => a.localeCompare(n)).map(([a, n]) => [a, [...n].sort()])
    )
  };
  return JSON.stringify({ targeting: t, value: e.value });
}
function Re(e) {
  const t = {
    schema: e.schema,
    issues: [],
    currentPath: ["flags"]
  };
  e.flags.forEach((s, l) => {
    ot(t, s, l);
  });
  const a = t.issues.filter((s) => s.severity === "error").length, n = t.issues.filter((s) => s.severity === "warning").length;
  return {
    valid: a === 0,
    issues: t.issues,
    errorCount: a,
    warningCount: n
  };
}
function ft(e) {
  return e.map((t, a) => typeof t == "number" ? `[${t}]` : a === 0 ? t : `.${t}`).join("");
}
function aa(e) {
  const t = e.severity === "error" ? "✗" : "⚠", a = ft(e.path);
  return `${t} ${a}: ${e.message}`;
}
function pt(e, t) {
  switch (t.type) {
    case "RESET":
      return fe(t.snapshot);
    case "UPDATE_FLAG":
      return k(e, (a) => {
        const n = a.current.flags.find((s) => s.key === t.key);
        n && (t.updater(n), a.modifiedFlags.add(t.key));
      });
    case "UPDATE_RULE":
      return k(e, (a) => {
        const n = a.current.flags.find((s) => s.key === t.flagKey);
        n && n.rules[t.ruleIndex] && (t.updater(n.rules[t.ruleIndex]), a.modifiedFlags.add(t.flagKey));
      });
    case "SET_FLAG_ACTIVE":
      return k(e, (a) => {
        const n = a.current.flags.find((s) => s.key === t.key);
        n && (n.isActive = t.isActive, a.modifiedFlags.add(t.key));
      });
    case "SET_FLAG_SALT":
      return k(e, (a) => {
        const n = a.current.flags.find((s) => s.key === t.key);
        n && (n.salt = t.salt, a.modifiedFlags.add(t.key));
      });
    case "SET_DEFAULT_VALUE":
      return k(e, (a) => {
        const n = a.current.flags.find((s) => s.key === t.key);
        n && n.type === t.value.type && (n.defaultValue = t.value, a.modifiedFlags.add(t.key));
      });
    case "SET_RULE_VALUE":
      return k(e, (a) => {
        const s = a.current.flags.find((l) => l.key === t.flagKey)?.rules[t.ruleIndex];
        s && s.value.type === t.value.type && (s.value = t.value, a.modifiedFlags.add(t.flagKey));
      });
    case "SET_RULE_RAMPUP":
      return k(e, (a) => {
        const s = a.current.flags.find((l) => l.key === t.flagKey)?.rules[t.ruleIndex];
        s && (s.rampUp = Math.max(0, Math.min(100, t.rampUp)), a.modifiedFlags.add(t.flagKey));
      });
    case "SET_RULE_PLATFORMS":
      return k(e, (a) => {
        const s = a.current.flags.find((l) => l.key === t.flagKey)?.rules[t.ruleIndex];
        s && (s.platforms = t.platforms, a.modifiedFlags.add(t.flagKey));
      });
    case "SET_RULE_LOCALES":
      return k(e, (a) => {
        const s = a.current.flags.find((l) => l.key === t.flagKey)?.rules[t.ruleIndex];
        s && (s.locales = t.locales, a.modifiedFlags.add(t.flagKey));
      });
    case "SET_RULE_VERSION_RANGE":
      return k(e, (a) => {
        const s = a.current.flags.find((l) => l.key === t.flagKey)?.rules[t.ruleIndex];
        s && (s.versionRange = t.versionRange, a.modifiedFlags.add(t.flagKey));
      });
    case "SET_RULE_AXES":
      return k(e, (a) => {
        const s = a.current.flags.find((l) => l.key === t.flagKey)?.rules[t.ruleIndex];
        s && (s.axes = t.axes, a.modifiedFlags.add(t.flagKey));
      });
    case "SET_RULE_ALLOWLIST":
      return k(e, (a) => {
        const s = a.current.flags.find((l) => l.key === t.flagKey)?.rules[t.ruleIndex];
        s && (s.rampUpAllowlist = t.allowlist, a.modifiedFlags.add(t.flagKey));
      });
    case "SET_RULE_NOTE":
      return k(e, (a) => {
        const s = a.current.flags.find((l) => l.key === t.flagKey)?.rules[t.ruleIndex];
        s && (s.note = t.note, a.modifiedFlags.add(t.flagKey));
      });
    case "SET_FLAG_ALLOWLIST":
      return k(e, (a) => {
        const n = a.current.flags.find((s) => s.key === t.key);
        n && (n.rampUpAllowlist = t.allowlist, a.modifiedFlags.add(t.key));
      });
    case "REVERT_FLAG":
      return k(e, (a) => {
        const n = e.original.flags.find((l) => l.key === t.key), s = a.current.flags.findIndex((l) => l.key === t.key);
        n && s !== -1 && (a.current.flags[s] = JSON.parse(JSON.stringify(n)), a.modifiedFlags.delete(t.key));
      });
    case "REVERT_ALL":
      return fe(e.original);
    default:
      return e;
  }
}
function k(e, t) {
  const a = Qe(e, (n) => {
    t(n), n.isDirty = n.modifiedFlags.size > 0;
  });
  return {
    ...a,
    validation: Re(a.current)
  };
}
function fe(e) {
  const t = JSON.parse(JSON.stringify(e)), a = JSON.parse(JSON.stringify(e));
  return {
    current: a,
    original: t,
    validation: Re(a),
    modifiedFlags: /* @__PURE__ */ new Set(),
    isDirty: !1
  };
}
function Le(e) {
  const t = [];
  for (const a of e.modifiedFlags) {
    const n = e.original.flags.find((l) => l.key === a), s = e.current.flags.find((l) => l.key === a);
    if (n && s) {
      const l = mt(n, s);
      l.length > 0 && t.push({
        key: a,
        type: "modified",
        before: n,
        after: s,
        changes: l
      });
    }
  }
  return {
    flags: t,
    hasChanges: t.length > 0
  };
}
function mt(e, t) {
  const a = [];
  e.isActive !== t.isActive && a.push({ path: "isActive", before: e.isActive, after: t.isActive }), e.salt !== t.salt && a.push({ path: "salt", before: e.salt, after: t.salt }), C(e.defaultValue, t.defaultValue) || a.push({ path: "defaultValue", before: e.defaultValue, after: t.defaultValue }), C(e.rampUpAllowlist, t.rampUpAllowlist) || a.push({ path: "rampUpAllowlist", before: e.rampUpAllowlist, after: t.rampUpAllowlist });
  const n = Math.max(e.rules.length, t.rules.length);
  for (let s = 0; s < n; s++) {
    const l = e.rules[s], i = t.rules[s];
    !l && i ? a.push({ path: `rules[${s}]`, before: void 0, after: i }) : l && !i ? a.push({ path: `rules[${s}]`, before: l, after: void 0 }) : l && i && !C(l, i) && (C(l.value, i.value) || a.push({ path: `rules[${s}].value`, before: l.value, after: i.value }), l.rampUp !== i.rampUp && a.push({ path: `rules[${s}].rampUp`, before: l.rampUp, after: i.rampUp }), C(l.platforms, i.platforms) || a.push({ path: `rules[${s}].platforms`, before: l.platforms, after: i.platforms }), C(l.locales, i.locales) || a.push({ path: `rules[${s}].locales`, before: l.locales, after: i.locales }), C(l.versionRange, i.versionRange) || a.push({ path: `rules[${s}].versionRange`, before: l.versionRange, after: i.versionRange }), C(l.axes, i.axes) || a.push({ path: `rules[${s}].axes`, before: l.axes, after: i.axes }), C(l.rampUpAllowlist, i.rampUpAllowlist) || a.push({ path: `rules[${s}].rampUpAllowlist`, before: l.rampUpAllowlist, after: i.rampUpAllowlist }), l.note !== i.note && a.push({ path: `rules[${s}].note`, before: l.note, after: i.note }));
  }
  return a;
}
function C(e, t) {
  return JSON.stringify(e) === JSON.stringify(t);
}
function Me(e, t) {
  return e.current.flags.find((a) => a.key === t);
}
function ht(e, t) {
  return e.original.flags.find((a) => a.key === t);
}
function Nt(e, t) {
  return e.modifiedFlags.has(t);
}
function gt(e, t) {
  const a = e.current.flags.findIndex((n) => n.key === t);
  return a === -1 ? [] : e.validation.issues.filter((n) => n.path[0] === "flags" && n.path[1] === a);
}
function kt(e) {
  return e.isDirty && e.validation.valid;
}
const Te = xe(null);
function yt({ snapshot: e, children: t }) {
  const [a, n] = Be(pt, e, fe), s = y(
    () => ({
      state: a,
      dispatch: n,
      schema: a.current.schema
    }),
    [a]
  );
  return /* @__PURE__ */ r(Te.Provider, { value: s, children: t });
}
function J() {
  const e = Pe(Te);
  if (!e)
    throw new Error("useEditorContext must be used within an EditorProvider");
  return e;
}
function _t() {
  const { state: e, dispatch: t, schema: a } = J(), n = u(
    (i) => t({ type: "RESET", snapshot: i }),
    [t]
  ), s = u(
    () => t({ type: "REVERT_ALL" }),
    [t]
  ), l = y(() => Le(e), [e]);
  return {
    snapshot: e.current,
    originalSnapshot: e.original,
    flags: e.current.flags,
    validation: e.validation,
    isDirty: e.isDirty,
    canSave: kt(e),
    diff: l,
    schema: a,
    reset: n,
    revertAll: s
  };
}
function vt(e) {
  const { state: t, dispatch: a, schema: n } = J(), s = y(() => Me(t, e), [t, e]), l = y(() => ht(t, e), [t, e]), i = y(() => Nt(t, e), [t, e]), o = y(
    () => gt(t, e),
    [t, e]
  ), p = u(
    (m) => a({ type: "SET_FLAG_ACTIVE", key: e, isActive: m }),
    [a, e]
  ), N = u(
    (m) => a({ type: "SET_FLAG_SALT", key: e, salt: m }),
    [a, e]
  ), S = u(
    (m) => a({ type: "SET_DEFAULT_VALUE", key: e, value: m }),
    [a, e]
  ), b = u(
    (m) => a({ type: "SET_FLAG_ALLOWLIST", key: e, allowlist: m }),
    [a, e]
  ), U = u(
    () => a({ type: "REVERT_FLAG", key: e }),
    [a, e]
  );
  return {
    flag: s,
    originalFlag: l,
    isModified: i,
    validationIssues: o,
    hasErrors: o.some((m) => m.severity === "error"),
    hasWarnings: o.some((m) => m.severity === "warning"),
    schema: n,
    setActive: p,
    setSalt: N,
    setDefaultValue: S,
    setAllowlist: b,
    revert: U
  };
}
function At(e, t) {
  const { state: a, dispatch: n, schema: s } = J(), l = y(() => Me(a, e), [a, e]), i = l?.rules[t], o = y(() => {
    const d = a.current.flags.findIndex((E) => E.key === e);
    return d === -1 ? [] : a.validation.issues.filter(
      (E) => E.path[0] === "flags" && E.path[1] === d && E.path[2] === "rules" && E.path[3] === t
    );
  }, [a, e, t]), p = u(
    (d) => n({ type: "SET_RULE_VALUE", flagKey: e, ruleIndex: t, value: d }),
    [n, e, t]
  ), N = u(
    (d) => n({ type: "SET_RULE_RAMPUP", flagKey: e, ruleIndex: t, rampUp: d }),
    [n, e, t]
  ), S = u(
    (d) => n({ type: "SET_RULE_PLATFORMS", flagKey: e, ruleIndex: t, platforms: d }),
    [n, e, t]
  ), b = u(
    (d) => n({ type: "SET_RULE_LOCALES", flagKey: e, ruleIndex: t, locales: d }),
    [n, e, t]
  ), U = u(
    (d) => n({ type: "SET_RULE_VERSION_RANGE", flagKey: e, ruleIndex: t, versionRange: d }),
    [n, e, t]
  ), m = u(
    (d) => n({ type: "SET_RULE_AXES", flagKey: e, ruleIndex: t, axes: d }),
    [n, e, t]
  ), w = u(
    (d) => n({ type: "SET_RULE_ALLOWLIST", flagKey: e, ruleIndex: t, allowlist: d }),
    [n, e, t]
  ), O = u(
    (d) => n({ type: "SET_RULE_NOTE", flagKey: e, ruleIndex: t, note: d }),
    [n, e, t]
  );
  return {
    rule: i,
    flagType: l?.type,
    validationIssues: o,
    hasErrors: o.some((d) => d.severity === "error"),
    schema: s,
    setValue: p,
    setRampUp: N,
    setPlatforms: S,
    setLocales: b,
    setVersionRange: U,
    setAxes: m,
    setAllowlist: w,
    setNote: O
  };
}
function na() {
  const { state: e } = J();
  return {
    validation: e.validation,
    isValid: e.validation.valid,
    errorCount: e.validation.errorCount,
    warningCount: e.validation.warningCount,
    issues: e.validation.issues
  };
}
function sa() {
  const { state: e } = J();
  return y(() => Le(e), [e]);
}
function pe({
  value: e,
  onChange: t,
  schema: a,
  compact: n = !1
}) {
  switch (e.type) {
    case "BOOLEAN":
      return /* @__PURE__ */ r(
        Et,
        {
          value: e,
          onChange: t,
          compact: n
        }
      );
    case "STRING":
      return /* @__PURE__ */ r(
        St,
        {
          value: e,
          onChange: t,
          compact: n
        }
      );
    case "INT":
      return /* @__PURE__ */ r(
        bt,
        {
          value: e,
          onChange: t,
          compact: n
        }
      );
    case "DOUBLE":
      return /* @__PURE__ */ r(
        Ut,
        {
          value: e,
          onChange: t,
          compact: n
        }
      );
    case "ENUM":
      return /* @__PURE__ */ r(
        wt,
        {
          value: e,
          onChange: t,
          schema: a,
          compact: n
        }
      );
    case "DATA_CLASS":
      return /* @__PURE__ */ r(
        Ct,
        {
          value: e,
          onChange: t,
          schema: a,
          compact: n
        }
      );
    default:
      return /* @__PURE__ */ r("span", { className: "ke-value-unknown", children: "Unknown type" });
  }
}
function Et({ value: e, onChange: t, compact: a }) {
  const n = u(
    (s) => {
      t({ type: "BOOLEAN", value: s });
    },
    [t]
  );
  return a ? /* @__PURE__ */ r("span", { className: `ke-value-boolean ${e.value ? "ke-value-true" : "ke-value-false"}`, children: e.value ? "true" : "false" }) : /* @__PURE__ */ c("label", { className: "ke-boolean-editor", children: [
    /* @__PURE__ */ r(
      "input",
      {
        type: "checkbox",
        checked: e.value,
        onChange: (s) => n(s.target.checked),
        className: "ke-checkbox"
      }
    ),
    /* @__PURE__ */ r("span", { className: "ke-boolean-label", children: e.value ? "true" : "false" })
  ] });
}
function St({ value: e, onChange: t, compact: a }) {
  const n = u(
    (s) => {
      t({ type: "STRING", value: s });
    },
    [t]
  );
  return a ? /* @__PURE__ */ c("span", { className: "ke-value-string", title: e.value, children: [
    '"',
    e.value.length > 30 ? e.value.slice(0, 30) + "..." : e.value,
    '"'
  ] }) : /* @__PURE__ */ r(
    "input",
    {
      type: "text",
      value: e.value,
      onChange: (s) => n(s.target.value),
      className: "ke-input ke-string-editor",
      placeholder: "Enter string value"
    }
  );
}
function bt({ value: e, onChange: t, compact: a }) {
  const n = u(
    (s) => {
      const l = Math.trunc(s);
      t({ type: "INT", value: l });
    },
    [t]
  );
  return a ? /* @__PURE__ */ r("span", { className: "ke-value-int", children: e.value }) : /* @__PURE__ */ r(
    "input",
    {
      type: "number",
      value: e.value,
      onChange: (s) => n(parseFloat(s.target.value) || 0),
      step: 1,
      className: "ke-input ke-number-editor"
    }
  );
}
function Ut({ value: e, onChange: t, compact: a }) {
  const n = u(
    (s) => {
      t({ type: "DOUBLE", value: s });
    },
    [t]
  );
  return a ? /* @__PURE__ */ r("span", { className: "ke-value-double", children: e.value }) : /* @__PURE__ */ r(
    "input",
    {
      type: "number",
      value: e.value,
      onChange: (s) => n(parseFloat(s.target.value) || 0),
      step: 0.01,
      className: "ke-input ke-number-editor"
    }
  );
}
function wt({ value: e, onChange: t, schema: a, compact: n }) {
  const s = y(() => a?.enums ? a.enums[e.enumClassName] ?? null : null, [a, e.enumClassName]), l = u(
    (i) => {
      t({
        type: "ENUM",
        value: i,
        enumClassName: e.enumClassName
      });
    },
    [t, e.enumClassName]
  );
  return n ? /* @__PURE__ */ r("span", { className: "ke-value-enum", title: e.enumClassName, children: e.value }) : s ? /* @__PURE__ */ c("div", { className: "ke-enum-editor", children: [
    /* @__PURE__ */ c(
      "select",
      {
        value: e.value,
        onChange: (i) => l(i.target.value),
        className: "ke-select",
        children: [
          !s.includes(e.value) && /* @__PURE__ */ c("option", { value: e.value, disabled: !0, children: [
            e.value,
            " (invalid)"
          ] }),
          s.map((i) => /* @__PURE__ */ r("option", { value: i, children: i }, i))
        ]
      }
    ),
    /* @__PURE__ */ r("span", { className: "ke-enum-class-name", title: e.enumClassName, children: e.enumClassName.split(".").pop() })
  ] }) : /* @__PURE__ */ c("div", { className: "ke-enum-editor ke-enum-editor--no-schema", children: [
    /* @__PURE__ */ r(
      "input",
      {
        type: "text",
        value: e.value,
        onChange: (i) => l(i.target.value),
        className: "ke-input"
      }
    ),
    /* @__PURE__ */ c("span", { className: "ke-enum-warning", children: [
      "Schema for ",
      e.enumClassName,
      " not found"
    ] })
  ] });
}
function Ct({
  value: e,
  onChange: t,
  schema: a,
  compact: n
}) {
  const s = y(() => a?.dataClasses ? a.dataClasses[e.dataClassName] ?? null : null, [a, e.dataClassName]), l = u(
    (i, o) => {
      t({
        type: "DATA_CLASS",
        dataClassName: e.dataClassName,
        value: {
          ...e.value,
          [i]: o
        }
      });
    },
    [t, e.dataClassName, e.value]
  );
  return n ? /* @__PURE__ */ c("span", { className: "ke-value-dataclass", title: JSON.stringify(e.value, null, 2), children: [
    e.dataClassName.split(".").pop(),
    " ",
    "{ ... }"
  ] }) : s ? /* @__PURE__ */ c("div", { className: "ke-dataclass-editor", children: [
    /* @__PURE__ */ r("span", { className: "ke-dataclass-name", children: e.dataClassName.split(".").pop() }),
    /* @__PURE__ */ r("div", { className: "ke-dataclass-fields", children: Object.entries(s.properties).map(([i, o]) => /* @__PURE__ */ r(
      Dt,
      {
        fieldName: i,
        fieldSchema: o,
        value: e.value[i],
        required: s.required.includes(i),
        onChange: (p) => l(i, p)
      },
      i
    )) })
  ] }) : /* @__PURE__ */ c("div", { className: "ke-dataclass-editor ke-dataclass-editor--no-schema", children: [
    /* @__PURE__ */ c("span", { className: "ke-dataclass-warning", children: [
      "Schema for ",
      e.dataClassName,
      " not found. Editing as raw JSON."
    ] }),
    /* @__PURE__ */ r(
      Fe,
      {
        value: e.value,
        onChange: (i) => t({
          type: "DATA_CLASS",
          dataClassName: e.dataClassName,
          value: i
        })
      }
    )
  ] });
}
function Dt({
  fieldName: e,
  fieldSchema: t,
  value: a,
  required: n,
  onChange: s
}) {
  return /* @__PURE__ */ c("div", { className: "ke-dataclass-field", children: [
    /* @__PURE__ */ c("label", { className: "ke-dataclass-field-label", children: [
      e,
      n && /* @__PURE__ */ r("span", { className: "ke-required-indicator", children: "*" })
    ] }),
    /* @__PURE__ */ r("div", { className: "ke-dataclass-field-input", children: (() => {
      switch (t.type) {
        case "boolean":
          return /* @__PURE__ */ r(
            "input",
            {
              type: "checkbox",
              checked: !!a,
              onChange: (i) => s(i.target.checked),
              className: "ke-checkbox"
            }
          );
        case "integer":
        case "number":
          return /* @__PURE__ */ r(
            "input",
            {
              type: "number",
              value: typeof a == "number" ? a : "",
              onChange: (i) => {
                const o = t.type === "integer" ? parseInt(i.target.value, 10) : parseFloat(i.target.value);
                s(isNaN(o) ? null : o);
              },
              step: t.type === "integer" ? 1 : 0.01,
              min: t.minimum,
              max: t.maximum,
              className: "ke-input ke-input-sm"
            }
          );
        case "string":
          return t.enum ? /* @__PURE__ */ c(
            "select",
            {
              value: String(a ?? ""),
              onChange: (i) => s(i.target.value),
              className: "ke-select ke-select-sm",
              children: [
                /* @__PURE__ */ r("option", { value: "", children: "—" }),
                t.enum.map((i) => /* @__PURE__ */ r("option", { value: i, children: i }, i))
              ]
            }
          ) : /* @__PURE__ */ r(
            "input",
            {
              type: "text",
              value: String(a ?? ""),
              onChange: (i) => s(i.target.value),
              minLength: t.minLength,
              maxLength: t.maxLength,
              pattern: t.pattern,
              className: "ke-input ke-input-sm"
            }
          );
        case "object":
        case "array":
          return /* @__PURE__ */ r(
            Fe,
            {
              value: a,
              onChange: s
            }
          );
        default:
          return /* @__PURE__ */ r(
            "input",
            {
              type: "text",
              value: String(a ?? ""),
              onChange: (i) => s(i.target.value),
              className: "ke-input ke-input-sm"
            }
          );
      }
    })() })
  ] });
}
function Fe({ value: e, onChange: t }) {
  const a = y(() => {
    try {
      return JSON.stringify(e, null, 2);
    } catch {
      return String(e);
    }
  }, [e]), n = u(
    (s) => {
      try {
        const l = JSON.parse(s);
        t(l);
      } catch {
      }
    },
    [t]
  );
  return /* @__PURE__ */ r(
    "textarea",
    {
      value: a,
      onChange: (s) => n(s.target.value),
      className: "ke-textarea ke-json-editor",
      rows: Math.min(10, Math.max(3, a.split(`
`).length)),
      spellCheck: !1
    }
  );
}
function Ot(e) {
  let t = 0;
  e.platforms.length > 0 && e.platforms.length < 3 && (t += e.platforms.length);
  const a = Array.isArray(e.locales) ? e.locales : [e.locales];
  t += a.length, e.versionRange.type !== "UNBOUNDED" && (t += e.versionRange.type === "MIN_AND_MAX_BOUND" ? 2 : 1);
  for (const n of Object.values(e.axes))
    t += n.length;
  return t;
}
function It({ flagKey: e, rules: t, flagType: a }) {
  const s = [...t.map((l, i) => ({
    rule: l,
    index: i,
    specificity: Ot(l)
  }))].sort(
    (l, i) => i.specificity - l.specificity
  );
  return /* @__PURE__ */ r("div", { className: "ke-rule-list", children: s.map(({ index: l, specificity: i }, o) => /* @__PURE__ */ r(
    Rt,
    {
      flagKey: e,
      ruleIndex: l,
      flagType: a,
      specificity: i,
      evaluationOrder: o + 1
    },
    l
  )) });
}
const Rt = Ee(function({
  flagKey: t,
  ruleIndex: a,
  flagType: n,
  // Reserved for type-specific rendering
  specificity: s,
  evaluationOrder: l
}) {
  const {
    rule: i,
    validationIssues: o,
    hasErrors: p,
    schema: N,
    setValue: S,
    setRampUp: b,
    setPlatforms: U,
    setLocales: m,
    setVersionRange: w,
    setAxes: O,
    setAllowlist: d,
    setNote: E
  } = At(t, a), [I, x] = T(!1);
  if (!i) return null;
  const P = u(() => {
    x((f) => !f);
  }, []), R = Array.isArray(i.locales) ? i.locales : [i.locales];
  return /* @__PURE__ */ c(
    "div",
    {
      className: `ke-rule-editor ${p ? "ke-rule-editor--error" : ""} ${I ? "ke-rule-editor--expanded" : ""}`,
      children: [
        /* @__PURE__ */ c("header", { className: "ke-rule-header", onClick: P, children: [
          /* @__PURE__ */ c("div", { className: "ke-rule-header-left", children: [
            /* @__PURE__ */ r("span", { className: "ke-expand-icon", children: I ? "▼" : "▶" }),
            /* @__PURE__ */ c("span", { className: "ke-rule-order", title: "Evaluation order (lower = checked first)", children: [
              "#",
              l
            ] }),
            /* @__PURE__ */ r(Lt, { score: s })
          ] }),
          /* @__PURE__ */ r("div", { className: "ke-rule-header-center", children: /* @__PURE__ */ c("span", { className: "ke-rule-summary", children: [
            i.platforms.length < 3 && /* @__PURE__ */ r("span", { className: "ke-rule-chip", children: i.platforms.join(", ") }),
            R.length <= 2 && /* @__PURE__ */ r("span", { className: "ke-rule-chip", children: R.map($e).join(", ") }),
            i.versionRange.type !== "UNBOUNDED" && /* @__PURE__ */ r("span", { className: "ke-rule-chip", children: Bt(i.versionRange) }),
            Object.keys(i.axes).length > 0 && /* @__PURE__ */ c("span", { className: "ke-rule-chip", children: [
              "+",
              Object.keys(i.axes).length,
              " axes"
            ] })
          ] }) }),
          /* @__PURE__ */ c("div", { className: "ke-rule-header-right", children: [
            /* @__PURE__ */ c("span", { className: "ke-rule-value-preview", children: [
              "→ ",
              /* @__PURE__ */ r(pe, { value: i.value, onChange: () => {
              }, compact: !0, schema: N })
            ] }),
            /* @__PURE__ */ c("span", { className: "ke-rule-rampup", children: [
              i.rampUp,
              "%"
            ] })
          ] })
        ] }),
        I && /* @__PURE__ */ c("div", { className: "ke-rule-body", children: [
          o.length > 0 && /* @__PURE__ */ r("div", { className: "ke-validation-issues", children: o.map((f, B) => /* @__PURE__ */ c(
            "div",
            {
              className: `ke-validation-issue ke-validation-issue--${f.severity}`,
              children: [
                /* @__PURE__ */ r("span", { className: "ke-validation-issue-icon", children: f.severity === "error" ? "✗" : "⚠" }),
                /* @__PURE__ */ r("span", { className: "ke-validation-issue-message", children: f.message })
              ]
            },
            B
          )) }),
          /* @__PURE__ */ c("div", { className: "ke-rule-row", children: [
            /* @__PURE__ */ r("label", { className: "ke-rule-label", children: "Note" }),
            /* @__PURE__ */ r(
              "input",
              {
                type: "text",
                value: i.note ?? "",
                onChange: (f) => E(f.target.value || void 0),
                placeholder: "Optional description",
                className: "ke-input"
              }
            )
          ] }),
          /* @__PURE__ */ c("div", { className: "ke-rule-row", children: [
            /* @__PURE__ */ r("label", { className: "ke-rule-label", children: "Value" }),
            /* @__PURE__ */ r(pe, { value: i.value, onChange: S, schema: N })
          ] }),
          /* @__PURE__ */ c("div", { className: "ke-rule-row", children: [
            /* @__PURE__ */ c("label", { className: "ke-rule-label", children: [
              "Ramp-up",
              /* @__PURE__ */ r("span", { className: "ke-rule-hint", children: "Percentage of matched users who receive this value" })
            ] }),
            /* @__PURE__ */ r(Mt, { value: i.rampUp, onChange: b })
          ] }),
          /* @__PURE__ */ c("div", { className: "ke-rule-row", children: [
            /* @__PURE__ */ r("label", { className: "ke-rule-label", children: "Platforms" }),
            /* @__PURE__ */ r(
              Ft,
              {
                selected: i.platforms,
                onChange: U
              }
            )
          ] }),
          /* @__PURE__ */ c("div", { className: "ke-rule-row", children: [
            /* @__PURE__ */ r("label", { className: "ke-rule-label", children: "Locales" }),
            /* @__PURE__ */ r(
              $t,
              {
                selected: R,
                onChange: m
              }
            )
          ] }),
          /* @__PURE__ */ c("div", { className: "ke-rule-row", children: [
            /* @__PURE__ */ r("label", { className: "ke-rule-label", children: "Version Range" }),
            /* @__PURE__ */ r(
              xt,
              {
                value: i.versionRange,
                onChange: w
              }
            )
          ] }),
          Object.keys(i.axes).length > 0 && /* @__PURE__ */ c("div", { className: "ke-rule-row", children: [
            /* @__PURE__ */ r("label", { className: "ke-rule-label", children: "Axes" }),
            /* @__PURE__ */ r(Pt, { value: i.axes, onChange: O })
          ] }),
          /* @__PURE__ */ c("div", { className: "ke-rule-row", children: [
            /* @__PURE__ */ c("label", { className: "ke-rule-label", children: [
              "Allowlist",
              /* @__PURE__ */ r("span", { className: "ke-rule-hint", children: "StableIds that bypass ramp-up for this rule" })
            ] }),
            /* @__PURE__ */ r(
              "textarea",
              {
                value: i.rampUpAllowlist.join(`
`),
                onChange: (f) => {
                  const B = f.target.value.split(`
`).map((h) => h.trim()).filter((h) => h.length > 0);
                  d(B);
                },
                placeholder: "One hex-encoded StableId per line",
                className: "ke-textarea",
                rows: 2
              }
            )
          ] })
        ] })
      ]
    }
  );
});
function Lt({ score: e }) {
  const a = Math.min(e, 5);
  return /* @__PURE__ */ r("span", { className: "ke-specificity", title: `Specificity score: ${e}`, children: Array.from({ length: 5 }, (n, s) => /* @__PURE__ */ r(
    "span",
    {
      className: `ke-specificity-star ${s < a ? "ke-specificity-star--filled" : ""}`,
      children: "★"
    },
    s
  )) });
}
function Mt({ value: e, onChange: t }) {
  return /* @__PURE__ */ c("div", { className: "ke-rampup-slider", children: [
    /* @__PURE__ */ r(
      "input",
      {
        type: "range",
        min: 0,
        max: 100,
        step: 1,
        value: e,
        onChange: (a) => t(parseInt(a.target.value, 10)),
        className: "ke-slider"
      }
    ),
    /* @__PURE__ */ r(
      "input",
      {
        type: "number",
        min: 0,
        max: 100,
        value: e,
        onChange: (a) => t(Math.max(0, Math.min(100, parseInt(a.target.value, 10) || 0))),
        className: "ke-input ke-input-sm ke-rampup-input"
      }
    ),
    /* @__PURE__ */ r("span", { className: "ke-rampup-unit", children: "%" })
  ] });
}
const Tt = ["IOS", "ANDROID", "WEB"];
function Ft({ selected: e, onChange: t }) {
  const a = u(
    (n) => {
      e.includes(n) ? t(e.filter((s) => s !== n)) : t([...e, n]);
    },
    [e, t]
  );
  return /* @__PURE__ */ r("div", { className: "ke-chip-selector", children: Tt.map((n) => /* @__PURE__ */ r(
    "button",
    {
      type: "button",
      className: `ke-chip ${e.includes(n) ? "ke-chip--selected" : ""}`,
      onClick: () => a(n),
      children: n
    },
    n
  )) });
}
const q = [
  "UNITED_STATES",
  "UNITED_KINGDOM",
  "CANADA",
  "AUSTRALIA",
  "GERMANY",
  "FRANCE",
  "JAPAN"
];
function $t({ selected: e, onChange: t }) {
  const [a, n] = T(!1), s = u(
    (i) => {
      e.includes(i) ? t(e.filter((o) => o !== i)) : t([...e, i]);
    },
    [e, t]
  ), l = a ? [...q, ...e.filter((i) => !q.includes(i))] : [.../* @__PURE__ */ new Set([...q, ...e])];
  return /* @__PURE__ */ r("div", { className: "ke-locale-selector", children: /* @__PURE__ */ c("div", { className: "ke-chip-selector ke-chip-selector--wrap", children: [
    l.map((i) => /* @__PURE__ */ r(
      "button",
      {
        type: "button",
        className: `ke-chip ${e.includes(i) ? "ke-chip--selected" : ""}`,
        onClick: () => s(i),
        children: $e(i)
      },
      i
    )),
    /* @__PURE__ */ r(
      "button",
      {
        type: "button",
        className: "ke-chip ke-chip--more",
        onClick: () => n(!a),
        children: a ? "Show less" : `+${26 - q.length} more`
      }
    )
  ] }) });
}
function xt({ value: e, onChange: t }) {
  const a = u(
    (l) => {
      switch (l) {
        case "UNBOUNDED":
          t({ type: "UNBOUNDED" });
          break;
        case "MIN_BOUND":
          t({
            type: "MIN_BOUND",
            min: "min" in e ? e.min : { major: 0, minor: 0, patch: 0 }
          });
          break;
        case "MAX_BOUND":
          t({
            type: "MAX_BOUND",
            max: "max" in e ? e.max : { major: 99, minor: 99, patch: 99 }
          });
          break;
        case "MIN_AND_MAX_BOUND":
          t({
            type: "MIN_AND_MAX_BOUND",
            min: "min" in e ? e.min : { major: 0, minor: 0, patch: 0 },
            max: "max" in e ? e.max : { major: 99, minor: 99, patch: 99 }
          });
          break;
      }
    },
    [e, t]
  ), n = u(
    (l) => {
      e.type === "MIN_BOUND" ? t({ type: "MIN_BOUND", min: l }) : e.type === "MIN_AND_MAX_BOUND" && t({ ...e, min: l });
    },
    [e, t]
  ), s = u(
    (l) => {
      e.type === "MAX_BOUND" ? t({ type: "MAX_BOUND", max: l }) : e.type === "MIN_AND_MAX_BOUND" && t({ ...e, max: l });
    },
    [e, t]
  );
  return /* @__PURE__ */ c("div", { className: "ke-version-range-editor", children: [
    /* @__PURE__ */ c("div", { className: "ke-version-type-selector", children: [
      /* @__PURE__ */ c("label", { className: "ke-radio-option", children: [
        /* @__PURE__ */ r(
          "input",
          {
            type: "radio",
            checked: e.type === "UNBOUNDED",
            onChange: () => a("UNBOUNDED")
          }
        ),
        /* @__PURE__ */ r("span", { children: "All versions" })
      ] }),
      /* @__PURE__ */ c("label", { className: "ke-radio-option", children: [
        /* @__PURE__ */ r(
          "input",
          {
            type: "radio",
            checked: e.type === "MIN_BOUND",
            onChange: () => a("MIN_BOUND")
          }
        ),
        /* @__PURE__ */ r("span", { children: "Minimum" })
      ] }),
      /* @__PURE__ */ c("label", { className: "ke-radio-option", children: [
        /* @__PURE__ */ r(
          "input",
          {
            type: "radio",
            checked: e.type === "MAX_BOUND",
            onChange: () => a("MAX_BOUND")
          }
        ),
        /* @__PURE__ */ r("span", { children: "Maximum" })
      ] }),
      /* @__PURE__ */ c("label", { className: "ke-radio-option", children: [
        /* @__PURE__ */ r(
          "input",
          {
            type: "radio",
            checked: e.type === "MIN_AND_MAX_BOUND",
            onChange: () => a("MIN_AND_MAX_BOUND")
          }
        ),
        /* @__PURE__ */ r("span", { children: "Range" })
      ] })
    ] }),
    (e.type === "MIN_BOUND" || e.type === "MIN_AND_MAX_BOUND") && /* @__PURE__ */ c("div", { className: "ke-version-input-row", children: [
      /* @__PURE__ */ r("span", { className: "ke-version-label", children: "Min:" }),
      /* @__PURE__ */ r(Ae, { value: e.min, onChange: n })
    ] }),
    (e.type === "MAX_BOUND" || e.type === "MIN_AND_MAX_BOUND") && /* @__PURE__ */ c("div", { className: "ke-version-input-row", children: [
      /* @__PURE__ */ r("span", { className: "ke-version-label", children: "Max:" }),
      /* @__PURE__ */ r(Ae, { value: e.max, onChange: s })
    ] })
  ] });
}
function Ae({ value: e, onChange: t }) {
  return /* @__PURE__ */ c("div", { className: "ke-version-input", children: [
    /* @__PURE__ */ r(
      "input",
      {
        type: "number",
        min: 0,
        value: e.major,
        onChange: (a) => t({ ...e, major: Math.max(0, parseInt(a.target.value, 10) || 0) }),
        className: "ke-input ke-input-xs",
        placeholder: "0"
      }
    ),
    /* @__PURE__ */ r("span", { className: "ke-version-separator", children: "." }),
    /* @__PURE__ */ r(
      "input",
      {
        type: "number",
        min: 0,
        value: e.minor,
        onChange: (a) => t({ ...e, minor: Math.max(0, parseInt(a.target.value, 10) || 0) }),
        className: "ke-input ke-input-xs",
        placeholder: "0"
      }
    ),
    /* @__PURE__ */ r("span", { className: "ke-version-separator", children: "." }),
    /* @__PURE__ */ r(
      "input",
      {
        type: "number",
        min: 0,
        value: e.patch,
        onChange: (a) => t({ ...e, patch: Math.max(0, parseInt(a.target.value, 10) || 0) }),
        className: "ke-input ke-input-xs",
        placeholder: "0"
      }
    )
  ] });
}
function Pt({ value: e }) {
  return /* @__PURE__ */ r("div", { className: "ke-axes-editor", children: Object.entries(e).map(([t, a]) => /* @__PURE__ */ c("div", { className: "ke-axis-row", children: [
    /* @__PURE__ */ c("span", { className: "ke-axis-key", children: [
      t,
      ":"
    ] }),
    /* @__PURE__ */ r("div", { className: "ke-chip-selector", children: a.map((n) => /* @__PURE__ */ r("span", { className: "ke-chip ke-chip--selected", children: n }, n)) })
  ] }, t)) });
}
function $e(e) {
  return {
    UNITED_STATES: "US",
    UNITED_KINGDOM: "UK",
    CANADA: "CA",
    CANADA_FRENCH: "CA-FR",
    AUSTRALIA: "AU",
    NEW_ZEALAND: "NZ",
    HONG_KONG: "HK",
    HONG_KONG_ENGLISH: "HK-EN",
    BELGIUM_DUTCH: "BE-NL",
    BELGIUM_FRENCH: "BE-FR"
  }[e] ?? e.replace(/_/g, " ");
}
function Bt(e) {
  switch (e.type) {
    case "UNBOUNDED":
      return "all versions";
    case "MIN_BOUND":
      return `≥${e.min.major}.${e.min.minor}.${e.min.patch}`;
    case "MAX_BOUND":
      return `≤${e.max.major}.${e.max.minor}.${e.max.patch}`;
    case "MIN_AND_MAX_BOUND":
      return `${e.min.major}.${e.min.minor}.${e.min.patch}–${e.max.major}.${e.max.minor}.${e.max.patch}`;
  }
}
function Vt({ before: e, after: t }) {
  const a = y(() => Wt(e, t), [e, t]);
  return a.length === 0 ? /* @__PURE__ */ r("div", { className: "ke-diff-inline ke-diff-empty", children: "No changes" }) : /* @__PURE__ */ r("div", { className: "ke-diff-inline", children: /* @__PURE__ */ c("table", { className: "ke-diff-table", children: [
    /* @__PURE__ */ r("thead", { children: /* @__PURE__ */ c("tr", { children: [
      /* @__PURE__ */ r("th", { className: "ke-diff-th", children: "Field" }),
      /* @__PURE__ */ r("th", { className: "ke-diff-th ke-diff-th--before", children: "Before" }),
      /* @__PURE__ */ r("th", { className: "ke-diff-th ke-diff-th--after", children: "After" })
    ] }) }),
    /* @__PURE__ */ r("tbody", { children: a.map((n, s) => /* @__PURE__ */ c("tr", { className: "ke-diff-row", children: [
      /* @__PURE__ */ r("td", { className: "ke-diff-path", children: n.path }),
      /* @__PURE__ */ r("td", { className: "ke-diff-before", children: /* @__PURE__ */ r(W, { value: n.before }) }),
      /* @__PURE__ */ r("td", { className: "ke-diff-after", children: /* @__PURE__ */ r(W, { value: n.after }) })
    ] }, s)) })
  ] }) });
}
function jt({
  diff: e,
  validation: t,
  isSaving: a,
  onConfirm: n,
  onCancel: s
}) {
  const l = t.warningCount > 0;
  return /* @__PURE__ */ r("div", { className: "ke-modal-overlay", onClick: s, children: /* @__PURE__ */ c("div", { className: "ke-modal", onClick: (i) => i.stopPropagation(), children: [
    /* @__PURE__ */ c("header", { className: "ke-modal-header", children: [
      /* @__PURE__ */ r("h2", { className: "ke-modal-title", children: "Review Changes" }),
      /* @__PURE__ */ r(
        "button",
        {
          type: "button",
          className: "ke-modal-close",
          onClick: s,
          "aria-label": "Close",
          children: "×"
        }
      )
    ] }),
    /* @__PURE__ */ c("div", { className: "ke-modal-body", children: [
      l && /* @__PURE__ */ c("div", { className: "ke-modal-section ke-modal-warnings", children: [
        /* @__PURE__ */ c("h3", { className: "ke-modal-section-title", children: [
          "⚠ ",
          t.warningCount,
          " Warning",
          t.warningCount !== 1 ? "s" : ""
        ] }),
        /* @__PURE__ */ r("ul", { className: "ke-warning-list", children: t.issues.filter((i) => i.severity === "warning").map((i, o) => /* @__PURE__ */ r("li", { className: "ke-warning-item", children: i.message }, o)) })
      ] }),
      /* @__PURE__ */ c("div", { className: "ke-modal-section", children: [
        /* @__PURE__ */ c("h3", { className: "ke-modal-section-title", children: [
          e.flags.length,
          " Flag",
          e.flags.length !== 1 ? "s" : "",
          " Modified"
        ] }),
        e.flags.length === 0 ? /* @__PURE__ */ r("p", { className: "ke-modal-empty", children: "No changes to save." }) : /* @__PURE__ */ r("div", { className: "ke-modal-diff-list", children: e.flags.map((i) => /* @__PURE__ */ r(Gt, { flagDiff: i }, i.key)) })
      ] })
    ] }),
    /* @__PURE__ */ c("footer", { className: "ke-modal-footer", children: [
      /* @__PURE__ */ r(
        "button",
        {
          type: "button",
          className: "ke-button ke-button-secondary",
          onClick: s,
          disabled: a,
          children: "Cancel"
        }
      ),
      /* @__PURE__ */ r(
        "button",
        {
          type: "button",
          className: "ke-button ke-button-primary",
          onClick: n,
          disabled: a || e.flags.length === 0,
          children: a ? "Saving..." : l ? "Save Anyway" : "Save"
        }
      )
    ] })
  ] }) });
}
function Gt({ flagDiff: e }) {
  const t = e.key.split("::").pop() ?? e.key;
  return /* @__PURE__ */ c("div", { className: "ke-flag-diff-summary", children: [
    /* @__PURE__ */ r("h4", { className: "ke-flag-diff-name", children: t }),
    /* @__PURE__ */ r("div", { className: "ke-flag-diff-changes", children: e.changes.map((a, n) => /* @__PURE__ */ c("div", { className: "ke-change-row", children: [
      /* @__PURE__ */ r("span", { className: "ke-change-path", children: a.path }),
      /* @__PURE__ */ r("span", { className: "ke-change-arrow", children: "→" }),
      /* @__PURE__ */ r("span", { className: "ke-change-before", children: /* @__PURE__ */ r(W, { value: a.before, compact: !0 }) }),
      /* @__PURE__ */ r("span", { className: "ke-change-to", children: "to" }),
      /* @__PURE__ */ r("span", { className: "ke-change-after", children: /* @__PURE__ */ r(W, { value: a.after, compact: !0 }) })
    ] }, n)) })
  ] });
}
function W({ value: e, compact: t = !1 }) {
  if (e === void 0)
    return /* @__PURE__ */ r("span", { className: "ke-diff-value ke-diff-value--undefined", children: "(none)" });
  if (e === null)
    return /* @__PURE__ */ r("span", { className: "ke-diff-value ke-diff-value--null", children: "null" });
  if (typeof e == "boolean")
    return /* @__PURE__ */ r("span", { className: `ke-diff-value ke-diff-value--boolean ${e ? "ke-true" : "ke-false"}`, children: String(e) });
  if (typeof e == "number")
    return /* @__PURE__ */ r("span", { className: "ke-diff-value ke-diff-value--number", children: e });
  if (typeof e == "string") {
    const a = t && e.length > 20 ? e.slice(0, 20) + "..." : e;
    return /* @__PURE__ */ c("span", { className: "ke-diff-value ke-diff-value--string", title: e, children: [
      '"',
      a,
      '"'
    ] });
  }
  return Array.isArray(e) ? t ? /* @__PURE__ */ c("span", { className: "ke-diff-value ke-diff-value--array", children: [
    "[",
    e.length,
    " items]"
  ] }) : /* @__PURE__ */ c("span", { className: "ke-diff-value ke-diff-value--array", children: [
    "[",
    e.map((a, n) => /* @__PURE__ */ c("span", { children: [
      n > 0 && ", ",
      /* @__PURE__ */ r(W, { value: a, compact: !0 })
    ] }, n)),
    "]"
  ] }) : typeof e == "object" ? "type" in e && typeof e.type == "string" ? /* @__PURE__ */ r(zt, { value: e, compact: t }) : "type" in e && ["UNBOUNDED", "MIN_BOUND", "MAX_BOUND", "MIN_AND_MAX_BOUND"].includes(e.type) ? /* @__PURE__ */ r(Xt, { value: e }) : t ? /* @__PURE__ */ r("span", { className: "ke-diff-value ke-diff-value--object", children: "{...}" }) : /* @__PURE__ */ r("span", { className: "ke-diff-value ke-diff-value--object", children: /* @__PURE__ */ r("pre", { children: JSON.stringify(e, null, 2) }) }) : /* @__PURE__ */ r("span", { className: "ke-diff-value", children: String(e) });
}
function zt({ value: e, compact: t }) {
  switch (e.type) {
    case "BOOLEAN":
      return /* @__PURE__ */ r("span", { className: `ke-diff-value ke-diff-value--boolean ${e.value ? "ke-true" : "ke-false"}`, children: String(e.value) });
    case "STRING":
      const a = t && e.value.length > 15 ? `"${e.value.slice(0, 15)}..."` : `"${e.value}"`;
      return /* @__PURE__ */ r("span", { className: "ke-diff-value ke-diff-value--string", children: a });
    case "INT":
    case "DOUBLE":
      return /* @__PURE__ */ r("span", { className: "ke-diff-value ke-diff-value--number", children: e.value });
    case "ENUM":
      return /* @__PURE__ */ r("span", { className: "ke-diff-value ke-diff-value--enum", children: e.value });
    case "DATA_CLASS":
      return /* @__PURE__ */ r("span", { className: "ke-diff-value ke-diff-value--dataclass", children: t ? "{...}" : JSON.stringify(e.value) });
  }
}
function Xt({ value: e }) {
  const t = (a) => `${a.major}.${a.minor}.${a.patch}`;
  switch (e.type) {
    case "UNBOUNDED":
      return /* @__PURE__ */ r("span", { className: "ke-diff-value", children: "all versions" });
    case "MIN_BOUND":
      return /* @__PURE__ */ c("span", { className: "ke-diff-value", children: [
        "≥",
        t(e.min)
      ] });
    case "MAX_BOUND":
      return /* @__PURE__ */ c("span", { className: "ke-diff-value", children: [
        "≤",
        t(e.max)
      ] });
    case "MIN_AND_MAX_BOUND":
      return /* @__PURE__ */ c("span", { className: "ke-diff-value", children: [
        t(e.min),
        "–",
        t(e.max)
      ] });
  }
}
function Wt(e, t) {
  const a = [];
  e.isActive !== t.isActive && a.push({ path: "isActive", before: e.isActive, after: t.isActive }), e.salt !== t.salt && a.push({ path: "salt", before: e.salt, after: t.salt }), D(e.defaultValue, t.defaultValue) || a.push({ path: "defaultValue", before: e.defaultValue, after: t.defaultValue }), D(e.rampUpAllowlist, t.rampUpAllowlist) || a.push({
    path: "rampUpAllowlist",
    before: e.rampUpAllowlist,
    after: t.rampUpAllowlist
  });
  const n = Math.max(e.rules.length, t.rules.length);
  for (let s = 0; s < n; s++) {
    const l = e.rules[s], i = t.rules[s];
    if (!l && i)
      a.push({ path: `rules[${s}]`, before: void 0, after: "added" });
    else if (l && !i)
      a.push({ path: `rules[${s}]`, before: "removed", after: void 0 });
    else if (l && i && !D(l, i)) {
      const o = Ht(l, i, s);
      a.push(...o);
    }
  }
  return a;
}
function Ht(e, t, a) {
  const n = [], s = `rules[${a}]`;
  return D(e.value, t.value) || n.push({ path: `${s}.value`, before: e.value, after: t.value }), e.rampUp !== t.rampUp && n.push({ path: `${s}.rampUp`, before: e.rampUp, after: t.rampUp }), D(e.platforms, t.platforms) || n.push({ path: `${s}.platforms`, before: e.platforms, after: t.platforms }), D(e.locales, t.locales) || n.push({ path: `${s}.locales`, before: e.locales, after: t.locales }), D(e.versionRange, t.versionRange) || n.push({
    path: `${s}.versionRange`,
    before: e.versionRange,
    after: t.versionRange
  }), D(e.axes, t.axes) || n.push({ path: `${s}.axes`, before: e.axes, after: t.axes }), D(e.rampUpAllowlist, t.rampUpAllowlist) || n.push({
    path: `${s}.rampUpAllowlist`,
    before: e.rampUpAllowlist,
    after: t.rampUpAllowlist
  }), e.note !== t.note && n.push({ path: `${s}.note`, before: e.note, after: t.note }), n;
}
function D(e, t) {
  return JSON.stringify(e) === JSON.stringify(t);
}
function Jt({ flags: e }) {
  return /* @__PURE__ */ r("div", { className: "ke-flag-list", children: e.map((t) => /* @__PURE__ */ r(Kt, { flagKey: t.key }, t.key)) });
}
const Kt = Ee(function({ flagKey: t }) {
  const {
    flag: a,
    originalFlag: n,
    isModified: s,
    validationIssues: l,
    hasErrors: i,
    hasWarnings: o,
    schema: p,
    setActive: N,
    setSalt: S,
    setDefaultValue: b,
    setAllowlist: U,
    revert: m
  } = vt(t), [w, O] = T(!1), [d, E] = T(!1), I = u(() => {
    O((f) => !f);
  }, []), x = u(() => {
    E((f) => !f);
  }, []);
  if (!a) return null;
  const P = a.key.split("::"), R = P.length >= 3 ? P[2] : a.key;
  return /* @__PURE__ */ c(
    "article",
    {
      className: `ke-flag-card ${w ? "ke-flag-card--expanded" : ""} ${i ? "ke-flag-card--error" : o ? "ke-flag-card--warning" : ""} ${s ? "ke-flag-card--modified" : ""}`,
      "data-flag-key": a.key,
      children: [
        /* @__PURE__ */ c("header", { className: "ke-flag-header", onClick: I, children: [
          /* @__PURE__ */ c("div", { className: "ke-flag-header-left", children: [
            /* @__PURE__ */ r("span", { className: `ke-expand-icon ${w ? "ke-expand-icon--open" : ""}`, children: "▶" }),
            /* @__PURE__ */ r(qt, { type: a.type }),
            /* @__PURE__ */ r("h3", { className: "ke-flag-name", children: R }),
            !a.isActive && /* @__PURE__ */ r("span", { className: "ke-status-badge ke-status-inactive", title: "Flag is inactive", children: "Inactive" }),
            s && /* @__PURE__ */ r("span", { className: "ke-status-badge ke-status-modified", title: "Has unsaved changes", children: "Modified" })
          ] }),
          /* @__PURE__ */ c("div", { className: "ke-flag-header-right", children: [
            /* @__PURE__ */ c("span", { className: "ke-rule-count", children: [
              a.rules.length,
              " rule",
              a.rules.length !== 1 ? "s" : ""
            ] }),
            i && /* @__PURE__ */ r("span", { className: "ke-validation-indicator ke-validation-indicator--error", title: "Has validation errors", children: "✗" }),
            !i && o && /* @__PURE__ */ r("span", { className: "ke-validation-indicator ke-validation-indicator--warning", title: "Has warnings", children: "⚠" })
          ] })
        ] }),
        w && /* @__PURE__ */ c("div", { className: "ke-flag-body", children: [
          l.length > 0 && /* @__PURE__ */ r("div", { className: "ke-validation-issues", children: l.map((f, B) => /* @__PURE__ */ c(
            "div",
            {
              className: `ke-validation-issue ke-validation-issue--${f.severity}`,
              children: [
                /* @__PURE__ */ r("span", { className: "ke-validation-issue-icon", children: f.severity === "error" ? "✗" : "⚠" }),
                /* @__PURE__ */ r("span", { className: "ke-validation-issue-message", children: f.message })
              ]
            },
            B
          )) }),
          s && n && /* @__PURE__ */ c("div", { className: "ke-diff-toggle", children: [
            /* @__PURE__ */ r(
              "button",
              {
                onClick: (f) => {
                  f.stopPropagation(), x();
                },
                className: "ke-button ke-button-text",
                children: d ? "Hide Changes" : "Show Changes"
              }
            ),
            /* @__PURE__ */ r(
              "button",
              {
                onClick: (f) => {
                  f.stopPropagation(), m();
                },
                className: "ke-button ke-button-text ke-button-danger",
                children: "Revert"
              }
            )
          ] }),
          d && n && /* @__PURE__ */ r(Vt, { before: n, after: a }),
          /* @__PURE__ */ c("section", { className: "ke-flag-settings", children: [
            /* @__PURE__ */ r("h4", { className: "ke-section-title", children: "Flag Settings" }),
            /* @__PURE__ */ c("div", { className: "ke-settings-grid", children: [
              /* @__PURE__ */ c("div", { className: "ke-setting-row", children: [
                /* @__PURE__ */ r("label", { className: "ke-setting-label", children: "Active" }),
                /* @__PURE__ */ r("div", { className: "ke-setting-control", children: /* @__PURE__ */ r(
                  Yt,
                  {
                    checked: a.isActive,
                    onChange: N,
                    label: a.isActive ? "Enabled" : "Disabled"
                  }
                ) })
              ] }),
              /* @__PURE__ */ c("div", { className: "ke-setting-row", children: [
                /* @__PURE__ */ c("label", { className: "ke-setting-label", children: [
                  "Salt",
                  /* @__PURE__ */ r("span", { className: "ke-setting-hint", children: "(changing redistributes ramp-up buckets)" })
                ] }),
                /* @__PURE__ */ r("div", { className: "ke-setting-control", children: /* @__PURE__ */ r(
                  "input",
                  {
                    type: "text",
                    value: a.salt,
                    onChange: (f) => S(f.target.value),
                    className: "ke-input ke-input-sm",
                    placeholder: "v1"
                  }
                ) })
              ] }),
              /* @__PURE__ */ c("div", { className: "ke-setting-row", children: [
                /* @__PURE__ */ r("label", { className: "ke-setting-label", children: "Default Value" }),
                /* @__PURE__ */ r("div", { className: "ke-setting-control", children: /* @__PURE__ */ r(
                  pe,
                  {
                    value: a.defaultValue,
                    onChange: b,
                    schema: p
                  }
                ) })
              ] }),
              a.rampUpAllowlist.length > 0 && /* @__PURE__ */ c("div", { className: "ke-setting-row", children: [
                /* @__PURE__ */ c("label", { className: "ke-setting-label", children: [
                  "Global Allowlist",
                  /* @__PURE__ */ c("span", { className: "ke-setting-hint", children: [
                    "(",
                    a.rampUpAllowlist.length,
                    " IDs)"
                  ] })
                ] }),
                /* @__PURE__ */ r("div", { className: "ke-setting-control", children: /* @__PURE__ */ r(
                  Zt,
                  {
                    value: a.rampUpAllowlist,
                    onChange: U
                  }
                ) })
              ] })
            ] })
          ] }),
          /* @__PURE__ */ c("section", { className: "ke-rules-section", children: [
            /* @__PURE__ */ c("h4", { className: "ke-section-title", children: [
              "Rules",
              /* @__PURE__ */ r("span", { className: "ke-section-subtitle", children: "(evaluated in specificity order, highest first)" })
            ] }),
            a.rules.length === 0 ? /* @__PURE__ */ r("p", { className: "ke-empty-rules", children: "No rules defined. All evaluations will return the default value." }) : /* @__PURE__ */ r(It, { flagKey: a.key, rules: a.rules, flagType: a.type })
          ] })
        ] })
      ]
    }
  );
});
function qt({ type: e }) {
  return /* @__PURE__ */ r("span", { className: `ke-type-badge ${{
    BOOLEAN: "ke-type-badge--boolean",
    STRING: "ke-type-badge--string",
    INT: "ke-type-badge--int",
    DOUBLE: "ke-type-badge--double",
    ENUM: "ke-type-badge--enum",
    DATA_CLASS: "ke-type-badge--dataclass"
  }[e]}`, children: e.toLowerCase() });
}
function Yt({ checked: e, onChange: t, label: a }) {
  return /* @__PURE__ */ c("label", { className: "ke-toggle", children: [
    /* @__PURE__ */ r(
      "input",
      {
        type: "checkbox",
        checked: e,
        onChange: (n) => t(n.target.checked),
        className: "ke-toggle-input"
      }
    ),
    /* @__PURE__ */ r("span", { className: "ke-toggle-track", children: /* @__PURE__ */ r("span", { className: "ke-toggle-thumb" }) }),
    a && /* @__PURE__ */ r("span", { className: "ke-toggle-label", children: a })
  ] });
}
function Zt({ value: e, onChange: t }) {
  const a = u(
    (n) => {
      const s = n.target.value.split(`
`).map((l) => l.trim()).filter((l) => l.length > 0);
      t(s);
    },
    [t]
  );
  return /* @__PURE__ */ r(
    "textarea",
    {
      value: e.join(`
`),
      onChange: a,
      placeholder: "One hex-encoded StableId per line",
      className: "ke-textarea ke-allowlist-editor",
      rows: Math.min(5, Math.max(2, e.length + 1))
    }
  );
}
function ra({
  snapshot: e,
  onSave: t,
  onChange: a,
  filter: n,
  theme: s = "system",
  className: l = ""
}) {
  return /* @__PURE__ */ r(yt, { snapshot: e, children: /* @__PURE__ */ r(
    Qt,
    {
      onSave: t,
      onChange: a,
      filter: n,
      theme: s,
      className: l
    }
  ) });
}
function Qt({
  onSave: e,
  onChange: t,
  // Reserved for future live preview
  filter: a,
  theme: n,
  className: s
}) {
  const {
    snapshot: l,
    flags: i,
    validation: o,
    isDirty: p,
    canSave: N,
    diff: S,
    revertAll: b
  } = _t(), [U, m] = T(!1), [w, O] = T(!1), [d, E] = T(""), I = y(() => {
    let h = i;
    if (a && (h = h.filter(a)), d.trim()) {
      const L = d.toLowerCase();
      h = h.filter(
        (j) => j.key.toLowerCase().includes(L) || j.type.toLowerCase().includes(L)
      );
    }
    return h;
  }, [i, a, d]), x = y(() => {
    const h = /* @__PURE__ */ new Map();
    for (const L of I) {
      const j = L.key.split("::"), ne = j.length >= 2 ? j[1] : "unknown";
      h.has(ne) || h.set(ne, []), h.get(ne).push(L);
    }
    return h;
  }, [I]), P = u(() => {
    N && m(!0);
  }, [N]), R = u(async () => {
    O(!0);
    try {
      await e(l), m(!1);
    } catch (h) {
      console.error("Save failed:", h);
    } finally {
      O(!1);
    }
  }, [l, e]), f = u(() => {
    m(!1);
  }, []);
  return /* @__PURE__ */ c("div", { className: `konditional-editor ${n === "system" ? "" : n === "dark" ? "dark" : "light"} ${s}`.trim(), children: [
    /* @__PURE__ */ c("header", { className: "ke-header", children: [
      /* @__PURE__ */ c("div", { className: "ke-header-left", children: [
        /* @__PURE__ */ r("h1", { className: "ke-title", children: "Configuration Editor" }),
        p && /* @__PURE__ */ r("span", { className: "ke-dirty-indicator", title: "Unsaved changes", children: "●" })
      ] }),
      /* @__PURE__ */ r("div", { className: "ke-header-center", children: /* @__PURE__ */ r(
        "input",
        {
          type: "search",
          placeholder: "Search flags...",
          value: d,
          onChange: (h) => E(h.target.value),
          className: "ke-search-input"
        }
      ) }),
      /* @__PURE__ */ c("div", { className: "ke-header-right", children: [
        o.errorCount > 0 && /* @__PURE__ */ c("span", { className: "ke-validation-badge ke-validation-error", children: [
          o.errorCount,
          " error",
          o.errorCount !== 1 ? "s" : ""
        ] }),
        o.warningCount > 0 && /* @__PURE__ */ c("span", { className: "ke-validation-badge ke-validation-warning", children: [
          o.warningCount,
          " warning",
          o.warningCount !== 1 ? "s" : ""
        ] }),
        /* @__PURE__ */ r(
          "button",
          {
            onClick: b,
            disabled: !p,
            className: "ke-button ke-button-secondary",
            children: "Revert All"
          }
        ),
        /* @__PURE__ */ r(
          "button",
          {
            onClick: P,
            disabled: !N,
            className: "ke-button ke-button-primary",
            title: p ? o.valid ? "Save changes" : "Fix validation errors before saving" : "No changes to save",
            children: "Save"
          }
        )
      ] })
    ] }),
    /* @__PURE__ */ r("main", { className: "ke-main", children: x.size === 0 ? /* @__PURE__ */ r("div", { className: "ke-empty-state", children: d ? `No flags match "${d}"` : "No flags in snapshot" }) : Array.from(x.entries()).map(([h, L]) => /* @__PURE__ */ c("section", { className: "ke-namespace-section", children: [
      /* @__PURE__ */ r("h2", { className: "ke-namespace-header", children: h }),
      /* @__PURE__ */ r(Jt, { flags: L })
    ] }, h)) }),
    U && /* @__PURE__ */ r(
      jt,
      {
        diff: S,
        validation: o,
        isSaving: w,
        onConfirm: R,
        onCancel: f
      }
    )
  ] });
}
const ia = [
  "AUSTRALIA",
  "AUSTRIA",
  "BELGIUM_DUTCH",
  "BELGIUM_FRENCH",
  "CANADA",
  "CANADA_FRENCH",
  "FINLAND",
  "FRANCE",
  "GERMANY",
  "HONG_KONG",
  "HONG_KONG_ENGLISH",
  "INDIA",
  "ITALY",
  "JAPAN",
  "MEXICO",
  "NETHERLANDS",
  "NEW_ZEALAND",
  "NORWAY",
  "SINGAPORE",
  "SPAIN",
  "SWEDEN",
  "TAIWAN",
  "UNITED_KINGDOM",
  "UNITED_STATES",
  "ICC_EN_EU",
  "ICC_EN_EI"
], la = ["IOS", "ANDROID", "WEB"];
function ca(e) {
  const t = e.split("::");
  return t.length !== 3 || t[0] !== "feature" ? null : {
    prefix: "feature",
    namespace: t[1],
    key: t[2]
  };
}
function oa(e, t) {
  return `feature::${e}::${t}`;
}
export {
  ia as APP_LOCALES,
  yt as EditorProvider,
  ra as KonditionalEditor,
  la as PLATFORMS,
  oa as formatFeatureId,
  aa as formatIssueForDisplay,
  ft as formatValidationPath,
  ca as parseFeatureId,
  sa as useDiff,
  _t as useEditor,
  vt as useFlag,
  At as useRule,
  na as useValidation,
  Re as validateSnapshot
};
