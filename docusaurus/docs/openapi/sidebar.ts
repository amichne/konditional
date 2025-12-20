import type { SidebarsConfig } from "@docusaurus/plugin-content-docs";

const sidebar: SidebarsConfig = {
  apisidebar: [
    {
      type: "doc",
      id: "openapi/konditional-serialization-schema",
    },
    {
      type: "category",
      label: "UNTAGGED",
      items: [
        {
          type: "doc",
          id: "openapi/fetch-a-configuration-snapshot",
          label: "Fetch a configuration snapshot",
          className: "api-method get",
        },
      ],
    },
  ],
};

export default sidebar.apisidebar;
