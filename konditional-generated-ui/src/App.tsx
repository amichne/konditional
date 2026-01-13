import {Toaster} from "@/components/ui/toaster";
import {Toaster as Sonner} from "@/components/ui/sonner";
import {TooltipProvider} from "@/components/ui/tooltip";
import {QueryClient, QueryClientProvider} from "@tanstack/react-query";
import {BrowserRouter, Route, Routes} from "react-router-dom";
import {AppShell} from "@/components/layout/AppShell";
import SchemaFormsPage from "@/pages/patterns/SchemaForms.tsx";

const queryClient = new QueryClient();

const App = () => (
  <QueryClientProvider client={queryClient}>
    <TooltipProvider>
      <Toaster />
      <Sonner />
      <BrowserRouter basename={import.meta.env.BASE_URL}>
        <AppShell>
          <Routes>
            <Route path="/" element={<SchemaFormsPage/>}/>
            {/*<Route path="/primitives" element={<PrimitivesColors />} />*/}
            {/*<Route path="/primitives/colors" element={<PrimitivesColors />} />*/}
            {/*<Route path="/primitives/typography" element={<PrimitivesTypography />} />*/}
            {/*<Route path="/primitives/spacing" element={<PrimitivesColors />} />*/}
            {/*<Route path="/primitives/motion" element={<PrimitivesColors />} />*/}
            {/*<Route path="/components" element={<ComponentsInputs />} />*/}
            {/*<Route path="/components/layout" element={<ComponentsInputs />} />*/}
            {/*<Route path="/components/navigation" element={<ComponentsInputs />} />*/}
            {/*<Route path="/components/overlays" element={<ComponentsInputs />} />*/}
            {/*<Route path="/components/inputs" element={<ComponentsInputs />} />*/}
            {/*<Route path="/components/data-display" element={<ComponentsInputs />} />*/}
            {/*<Route path="/patterns" element={<SchemaFormsPage />} />*/}
            {/*<Route path="/patterns/schema-forms" element={<SchemaFormsPage />} />*/}
            {/*<Route path="/patterns/safe-publishing" element={<Index />} />*/}
            {/*<Route path="/patterns/large-datasets" element={<Index />} />*/}
            {/*<Route path="/playground" element={<Index />} />*/}
            {/*<Route path="/demo" element={<Index />} />*/}
            {/*<Route path="*" element={<NotFound />} />*/}
          </Routes>
        </AppShell>
      </BrowserRouter>
    </TooltipProvider>
  </QueryClientProvider>
);

export default App;
