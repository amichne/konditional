import React, { createContext, useContext, useState, ReactNode } from "react";
import { createPortal } from "react-dom";

type DialogContextValue = {
  openDialogId: string | null;
  setOpenDialogId: (id: string | null) => void;
};

const DialogContext = createContext<DialogContextValue | null>(null);

export function InlayHintDialogProvider({ children }: { children: ReactNode }) {
  const [openDialogId, setOpenDialogId] = useState<string | null>(null);
  return (
    <DialogContext.Provider value={{ openDialogId, setOpenDialogId }}>
      {children}
    </DialogContext.Provider>
  );
}

function useDialogContext() {
  const context = useContext(DialogContext);
  if (!context) {
    throw new Error("InlayHint must be used within InlayHintDialogProvider");
  }
  return context;
}

export type InlayHintProps = {
  id: string;
  text: string;
  position: "before" | "after";
  dialogContent?: ReactNode;
  isInteractive?: boolean;
};

function Dialog({ id, content, onClose }: { id: string; content: ReactNode; onClose: () => void }) {
  const dialogStyle: React.CSSProperties = {
    position: "fixed",
    right: 0,
    top: 0,
    bottom: 0,
    width: "400px",
    backgroundColor: "var(--ifm-background-surface-color)",
    borderLeft: "1px solid var(--ifm-color-emphasis-300)",
    boxShadow: "-2px 0 8px rgba(0, 0, 0, 0.1)",
    padding: "1.5rem",
    overflowY: "auto",
    zIndex: 1000,
  };

  const closeButtonStyle: React.CSSProperties = {
    position: "absolute",
    top: "1rem",
    right: "1rem",
    background: "none",
    border: "none",
    fontSize: "1.5rem",
    cursor: "pointer",
    padding: "0.25rem 0.5rem",
    lineHeight: 1,
  };

  const overlayStyle: React.CSSProperties = {
    position: "fixed",
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    backgroundColor: "rgba(0, 0, 0, 0.3)",
    zIndex: 999,
  };

  return createPortal(
    <>
      <div style={overlayStyle} onClick={onClose} />
      <div style={dialogStyle}>
        <button type="button" onClick={onClose} style={closeButtonStyle} aria-label="Close dialog">
          ×
        </button>
        <div style={{ marginTop: "2rem" }}>{content}</div>
      </div>
    </>,
    document.body
  );
}

export function InlayHint({ id, text, position, dialogContent, isInteractive = false }: InlayHintProps) {
  const { openDialogId, setOpenDialogId } = useDialogContext();
  const isOpen = openDialogId === id;

  const handleClick = () => {
    if (dialogContent) {
      setOpenDialogId(isOpen ? null : id);
    }
  };

  const hasDialog = !!dialogContent;
  const shouldBeInteractive = isInteractive || hasDialog;

  const style: React.CSSProperties = {
    opacity: 0.55,
    fontSize: "0.9em",
    fontStyle: "italic",
    marginLeft: position === "after" ? 4 : 0,
    marginRight: position === "before" ? 4 : 0,
    cursor: shouldBeInteractive ? "pointer" : "default",
    userSelect: "none",
    textDecoration: isOpen ? "underline" : "none",
  };

  const content = (
    <>
      {shouldBeInteractive && handleClick ? (
        <button
          type="button"
          onClick={handleClick}
          style={{ ...style, background: "none", border: "none", padding: 0 }}
        >
          {text}
        </button>
      ) : (
        <span style={style}>{text}</span>
      )}
      {isOpen && dialogContent && <Dialog id={id} content={dialogContent} onClose={() => setOpenDialogId(null)} />}
    </>
  );

  return content;
}
