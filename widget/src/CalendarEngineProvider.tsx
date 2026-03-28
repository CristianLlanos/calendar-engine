import { createContext, useContext, useMemo, type ReactNode } from "react";
import { createClient, type CalendarEngineClient } from "@calendarengine/sdk";
import { defaultTheme, type CalendarTheme } from "./themes";

export type CalendarRole = "owner" | "member" | "viewer";

interface CalendarEngineContextValue {
  client: CalendarEngineClient;
  role: CalendarRole;
}

const CalendarEngineContext = createContext<CalendarEngineContextValue | null>(null);

export interface CalendarEngineProviderProps {
  baseUrl: string;
  token?: string;
  tenantId?: number;
  role?: CalendarRole;
  theme?: Partial<CalendarTheme>;
  children: ReactNode;
}

export function CalendarEngineProvider({
  baseUrl,
  token,
  tenantId,
  role = "viewer",
  theme,
  children,
}: CalendarEngineProviderProps) {
  const client = useMemo(
    () =>
      createClient({
        baseUrl,
        getToken: token ? () => token : undefined,
        getTenantId: tenantId ? () => tenantId : undefined,
      }),
    [baseUrl, token, tenantId],
  );

  const mergedTheme = useMemo(() => ({ ...defaultTheme, ...theme }), [theme]);

  const style = useMemo(() => {
    const vars: Record<string, string> = {};
    for (const [key, value] of Object.entries(mergedTheme)) {
      vars[key] = value;
    }
    return vars;
  }, [mergedTheme]);

  return (
    <CalendarEngineContext.Provider value={{ client, role }}>
      <div className="ce-root" style={style}>
        {children}
      </div>
    </CalendarEngineContext.Provider>
  );
}

export function useCalendarEngine(): CalendarEngineContextValue {
  const context = useContext(CalendarEngineContext);
  if (!context) {
    throw new Error("useCalendarEngine must be used within a CalendarEngineProvider");
  }
  return context;
}
