import { z } from 'zod';
import { AidCategory, UrgencyLevel, DonationType } from './constants';

export const donationSchema = z
  .object({
    type: z.enum([DonationType.MONETARY, DonationType.IN_KIND]),
    disasterId: z.string().uuid('Please select a valid disaster event'),
    amount: z.coerce.number().optional(),
    currency: z.string().default('USD'),
    items: z
      .array(
        z.object({
          category: z.nativeEnum(AidCategory),
          itemName: z.string().min(2, 'Item name must have at least 2 characters'),
          quantity: z.coerce.number().positive('Quantity must be greater than 0'),
          unit: z.string().min(1, 'Unit required (e.g. kg, boxes, liters)'),
          weightKg: z.coerce.number().min(0).optional(),
        })
      )
      .optional(),
  })
  .superRefine((data, ctx) => {
    if (data.type === DonationType.MONETARY) {
      if (!data.amount || data.amount <= 0) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          path: ['amount'],
          message: 'Donation amount must be greater than $0',
        });
      }
    } else if (data.type === DonationType.IN_KIND) {
      if (!data.items || data.items.length === 0) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          path: ['items'],
          message: 'At least one in-kind donation item is required',
        });
      }
    }
  });

export const aidRequestSchema = z.object({
  disasterId: z.string().uuid('Please select a valid disaster event'),
  category: z.nativeEnum(AidCategory, {
    errorMap: () => ({ message: 'Please select a valid aid category' }),
  }),
  itemName: z.string().min(2, 'Item name is required'),
  quantity: z.coerce.number().positive('Quantity must be greater than 0'),
  unit: z.string().min(1, 'Unit is required (e.g. kits, blankets, kg)'),
  urgency: z.nativeEnum(UrgencyLevel, {
    errorMap: () => ({ message: 'Please select an urgency level' }),
  }),
  latitude: z.coerce
    .number()
    .min(-90, 'Latitude must be between -90 and 90')
    .max(90, 'Latitude must be between -90 and 90'),
  longitude: z.coerce
    .number()
    .min(-180, 'Longitude must be between -180 and 180')
    .max(180, 'Longitude must be between -180 and 180'),
  locationName: z.string().min(3, 'Location description/camp name is required'),
  contactPhone: z.string().min(6, 'Contact phone number is required'),
  notes: z.string().max(500).optional(),
});

export const deliveryConfirmationSchema = z.object({
  shortageQuantity: z.coerce.number().min(0).default(0),
  damageNotes: z.string().max(1000).optional(),
  proofOfDeliveryUrl: z.string().url('Must be a valid image or document URL').or(z.string().min(5)),
  recipientSignature: z.string().min(2, 'Recipient signature/name is required'),
});

export const reportClosureSchema = z.object({
  reason: z.string().min(5, 'Please provide a clear reason for closure/delay'),
  location: z.string().min(3, 'Location of blockage or incident is required'),
  estimatedDelayMinutes: z.coerce.number().min(5, 'Delay must be at least 5 minutes'),
  alternativeRouteNotes: z.string().optional(),
});

export const allocationApprovalSchema = z.object({
  notes: z.string().max(500).optional(),
});

export const allocationRejectionSchema = z.object({
  reason: z.string().min(5, 'Rejection reason is required (min 5 characters)'),
});
